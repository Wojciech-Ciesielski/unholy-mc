"""Mały silnik syntezy dźwięków (numpy) + zapis OGG przez ffmpeg.

Wszystko w float32, mono lub stereo (kształt (n,) albo (n, 2)), SR = 44100.
"""
import subprocess, tempfile, wave
from pathlib import Path
import numpy as np

SR = 44100
rng = np.random.default_rng(0)


def seed(s):
    global rng
    rng = np.random.default_rng(s)


def t(dur):
    return np.arange(int(dur * SR)) / SR


def silence(dur):
    return np.zeros(int(dur * SR), np.float32)


# ------------------------------------------------------------------ źródła
def noise(dur, color="white"):
    n = rng.standard_normal(int(dur * SR))
    if color == "white":
        return n
    f = np.fft.rfftfreq(len(n), 1 / SR)
    spec = np.fft.rfft(n)
    f[0] = f[1] if len(f) > 1 else 1
    spec /= np.sqrt(f) if color == "pink" else f  # brown = 1/f
    out = np.fft.irfft(spec, len(n))
    return out / (np.abs(out).max() + 1e-9)


def phase(freq, dur):
    """freq: liczba albo tablica (glide). Zwraca fazę [rad]."""
    n = int(dur * SR)
    f = np.full(n, freq, float) if np.isscalar(freq) else np.interp(np.linspace(0, 1, n), np.linspace(0, 1, len(freq)), freq)
    return 2 * np.pi * np.cumsum(f) / SR


def sine(freq, dur):
    return np.sin(phase(freq, dur))


def saw(freq, dur):
    p = phase(freq, dur) / (2 * np.pi)
    return 2 * (p - np.floor(p + 0.5))


def square(freq, dur):
    return np.sign(sine(freq, dur))


def glide(a, b, n=64, curve=1.0):
    x = np.linspace(0, 1, n) ** curve
    return a + (b - a) * x


def vibrato(base, dur, rate, depth, jitter=0.0):
    tt = t(dur)
    f = base * (1 + depth * np.sin(2 * np.pi * rate * tt))
    if jitter:
        f *= 1 + jitter * smooth_noise(dur, 12)
    return f


def smooth_noise(dur, rate):
    """Losowa, płynna krzywa w [-1, 1] o mniej więcej `rate` zmianach na sekundę."""
    n = int(dur * SR)
    k = max(2, int(dur * rate) + 2)
    pts = rng.uniform(-1, 1, k)
    return np.interp(np.linspace(0, k - 1, n), np.arange(k), pts)


# ------------------------------------------------------------------ obwiednie
def env(n_or_sig, a=0.005, d=0.1, s=1.0, r=0.1, hold=None):
    n = n_or_sig if isinstance(n_or_sig, int) else len(n_or_sig)
    A, D, R = int(a * SR), int(d * SR), int(r * SR)
    H = n - A - D - R if hold is None else int(hold * SR)
    H = max(0, H)
    e = np.concatenate([np.linspace(0, 1, A, endpoint=False), np.linspace(1, s, D, endpoint=False),
                        np.full(H, s), np.linspace(s, 0, R)])
    e = np.pad(e, (0, max(0, n - len(e))))[:n]
    return e


def decay(n_or_sig, tau):
    n = n_or_sig if isinstance(n_or_sig, int) else len(n_or_sig)
    return np.exp(-np.arange(n) / SR / tau)


def fade(sig, fin=0.01, fout=0.05):
    sig = sig.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a: sig[:a] *= np.linspace(0, 1, a)[:, None] if sig.ndim == 2 else np.linspace(0, 1, a)
    if b: sig[-b:] *= np.linspace(1, 0, b)[:, None] if sig.ndim == 2 else np.linspace(1, 0, b)
    return sig


# ------------------------------------------------------------------ filtry (FFT)
def _mask(sig, fn):
    spec = np.fft.rfft(sig, axis=0)
    f = np.fft.rfftfreq(sig.shape[0], 1 / SR)
    m = fn(f)
    spec *= m[:, None] if sig.ndim == 2 else m
    return np.fft.irfft(spec, sig.shape[0], axis=0)


def lowpass(sig, fc, order=2):
    return _mask(sig, lambda f: 1 / np.sqrt(1 + (f / fc) ** (2 * order)))


def highpass(sig, fc, order=2):
    return _mask(sig, lambda f: 1 / np.sqrt(1 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(sig, fc, q=2.0):
    bw = fc / q
    return _mask(sig, lambda f: 1 / (1 + ((f - fc) / (bw / 2)) ** 2))


VOWELS = {  # (F1, F2, F3) Hz
    "a": (800, 1150, 2900), "e": (400, 2000, 2550), "i": (300, 2300, 3000),
    "o": (450, 800, 2830), "u": (325, 700, 2530), "uh": (600, 1000, 2400), "ee": (270, 2290, 3010),
}


def formant(sig, vowel="a", q=6.0, shift=1.0):
    fs = VOWELS[vowel] if isinstance(vowel, str) else vowel
    gains = (1.0, 0.6, 0.3)
    return sum(g * bandpass(sig, f * shift, q) for f, g in zip(fs, gains))


def formant_morph(sig, vowels, q=6.0, shift=1.0, frame=2048):
    """Filtr formantowy zmieniający samogłoski w czasie (STFT). vowels: lista samogłosek równomiernie w czasie."""
    hop = frame // 4
    win = np.hanning(frame)
    n = len(sig)
    pad = np.pad(sig, (frame, frame))
    out = np.zeros_like(pad)
    norm = np.zeros_like(pad)
    f = np.fft.rfftfreq(frame, 1 / SR)
    tracks = np.array([VOWELS[v] if isinstance(v, str) else v for v in vowels], float)
    nframes = (len(pad) - frame) // hop + 1
    for i in range(nframes):
        s0 = i * hop
        pos = np.clip((s0 - frame) / max(1, n), 0, 1) * (len(tracks) - 1)
        lo = int(np.floor(pos)); hi = min(lo + 1, len(tracks) - 1); w = pos - lo
        fs = (tracks[lo] * (1 - w) + tracks[hi] * w) * shift
        m = sum(g / (1 + ((f - fc) / (fc / q / 2)) ** 2) for fc, g in zip(fs, (1.0, 0.6, 0.3)))
        seg = np.fft.irfft(np.fft.rfft(pad[s0:s0 + frame] * win) * m, frame)
        out[s0:s0 + frame] += seg * win
        norm[s0:s0 + frame] += win ** 2
    out /= np.maximum(norm, 1e-6)
    return out[frame:frame + n]


# ------------------------------------------------------------------ efekty
def reverb(sig, size=1.5, wet=0.35, damp=4000, predelay=0.01, stereo=False):
    n_ir = int(size * SR)
    def ir():
        i = rng.standard_normal(n_ir) * np.exp(-np.arange(n_ir) / SR / (size / 6))
        i = lowpass(i, damp, 1)
        i = np.concatenate([np.zeros(int(predelay * SR)), i])
        return i / np.sqrt((i ** 2).sum())
    def conv(x, h):
        L = len(x) + len(h) - 1
        N = 1 << (L - 1).bit_length()
        return np.fft.irfft(np.fft.rfft(x, N) * np.fft.rfft(h, N), N)[:L]
    mono = sig if sig.ndim == 1 else sig.mean(1)
    if stereo or sig.ndim == 2:
        l, r = conv(mono, ir()), conv(mono, ir())
        dry = np.pad(sig if sig.ndim == 2 else np.stack([sig, sig], 1), ((0, len(l) - len(sig)), (0, 0)))
        return dry * (1 - wet) + np.stack([l, r], 1) * wet * 0.5
    w = conv(mono, ir())
    return np.pad(sig, (0, len(w) - len(sig))) * (1 - wet) + w * wet * 0.5


def distort(sig, drive=4.0):
    return np.tanh(sig * drive) / np.tanh(drive)


def bitcrush(sig, bits=6, down=4):
    q = 2 ** (bits - 1)
    out = np.round(sig * q) / q
    if down > 1:
        out = np.repeat(out[::down], down, axis=0)[:len(sig)]
    return out


def ring(sig, freq):
    return sig * sine(freq, len(sig) / SR)


def reverse(sig):
    return sig[::-1].copy()


def pitch(sig, factor):
    """Prosta zmiana wysokości przez resampling (zmienia też długość)."""
    n = int(len(sig) / factor)
    return np.interp(np.arange(n) * factor, np.arange(len(sig)), sig)


def mix(*parts):
    """mix((sig, offset_s, gain), ...) albo samych sygnałów."""
    items = [(p, 0.0, 1.0) if isinstance(p, np.ndarray) else (p[0], p[1] if len(p) > 1 else 0.0, p[2] if len(p) > 2 else 1.0) for p in parts]
    st = any(s.ndim == 2 for s, _, _ in items)
    n = max(int(o * SR) + len(s) for s, o, _ in items)
    out = np.zeros((n, 2) if st else n)
    for s, o, g in items:
        if st and s.ndim == 1: s = np.stack([s, s], 1)
        i = int(o * SR); out[i:i + len(s)] += s * g
    return out


def pan(sig, p):
    """p: -1 (lewo) .. 1 (prawo), albo tablica."""
    p = np.asarray(p)
    if p.ndim and len(p) != len(sig):
        p = np.interp(np.linspace(0, 1, len(sig)), np.linspace(0, 1, len(p)), p)
    a = (p + 1) * np.pi / 4
    return np.stack([sig * np.cos(a), sig * np.sin(a)], 1)


def loop_crossfade(sig, xf=0.5):
    """Bezszwowa pętla: koniec przenikany w początek."""
    n = int(xf * SR)
    head, body, tail = sig[:n], sig[n:-n] if n else sig, sig[-n:]
    w = np.linspace(0, 1, n)
    if sig.ndim == 2: w = w[:, None]
    return np.concatenate([tail * (1 - w) + head * w, body])


# ------------------------------------------------------------------ normalizacja i zapis
def normalize(sig, rms_db=-18.0, peak_db=-1.0):
    sig = sig - (sig.mean(0) if len(sig) else 0)
    rms = np.sqrt(np.mean(sig ** 2)) + 1e-9
    sig = sig * (10 ** (rms_db / 20) / rms)
    peak = 10 ** (peak_db / 20)
    over = np.abs(sig).max()
    if over > peak:  # miękki limiter
        sig = np.tanh(sig / peak) * peak
    return sig


def write_ogg(sig, path, quality=5, rms_db=-18.0, peak_db=-1.0, norm=True, stereo=None):
    path = Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    if stereo is False and sig.ndim == 2: sig = sig.mean(1)
    if stereo is True and sig.ndim == 1: sig = np.stack([sig, sig], 1)
    if norm: sig = normalize(sig, rms_db, peak_db)
    sig = np.clip(sig, -1, 1)
    ch = 2 if sig.ndim == 2 else 1
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(ch); w.setsampwidth(2); w.setframerate(SR)
            w.writeframes((sig * 32767).astype("<i2").tobytes())
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", str(quality),
                        "-map_metadata", "-1", str(path)], check=True)
        Path(tmp.name).unlink()
    return path
