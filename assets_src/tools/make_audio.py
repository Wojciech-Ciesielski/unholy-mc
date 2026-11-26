"""Generuje wszystkie pliki .ogg z sounds.json (synteza proceduralna + przerobiona płyta 11).

Użycie:  python3 assets_src/tools/make_audio.py [filtr]      np. "slenderman" – tylko pasujące zdarzenia
Płyta 11: plik 11.ogg z oficjalnych assetów (minecraft/sounds/records/11.ogg) w assets_src/audio_src/ (nie w repo)
albo podany zmienną ELEVEN=/ścieżka/11.ogg.
"""
import os, subprocess, sys, zlib
from pathlib import Path
import numpy as np
from sfx import *
import sfx
from make_sounds import SOUNDS

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/unholymc/sounds"


# =================================================================== klocki
def scream(dur, f0=700, vowels=("a", "a", "e"), rough=0.5, rise=1.3, vib=7.0, drive=3.0):
    f = vibrato(1, dur, vib, 0.04, jitter=0.03) * np.interp(np.linspace(0, 1, int(dur * SR)), [0, 0.15, 1], [f0, f0 * rise, f0 * 0.8])
    src = saw(f, dur) * 0.7 + saw(f * 1.007, dur) * 0.5 + noise(dur) * rough
    v = formant_morph(src, list(vowels), q=5)
    return distort(v * env(len(v), 0.004, 0.08, 0.8, dur * 0.5), drive)


def whisper(dur, syll=None, vowels="aeiouh", brightness=1.0):
    n = int(dur * SR)
    syll = syll or max(2, int(dur * 4))
    amp = np.zeros(n)
    for i in range(syll):
        c = int(sfx.rng.uniform(0.05, 0.95) * n); w = int(sfx.rng.uniform(0.06, 0.18) * SR)
        a, b = max(0, c - w), min(n, c + w)
        amp[a:b] = np.maximum(amp[a:b], np.hanning(b - a) * sfx.rng.uniform(0.5, 1))
    vs = [str(sfx.rng.choice(["a", "e", "i", "o", "u", "uh"])) for _ in range(syll + 1)]
    breath = formant_morph(noise(dur, "pink"), vs, q=4, shift=1.1 * brightness)
    hiss = highpass(noise(dur), 4500) * 0.25
    s_amp = np.zeros(n)
    for _ in range(max(1, syll // 2)):
        c = int(sfx.rng.uniform(0.1, 0.9) * n); w = int(0.05 * SR)
        a, b = max(0, c - w), min(n, c + w); s_amp[a:b] = np.maximum(s_amp[a:b], np.hanning(b - a))
    return highpass(breath * amp + hiss * (amp * 0.4 + s_amp), 300)


def crackle(dur, density=200, bright=3000):
    n = int(dur * SR)
    x = np.zeros(n)
    idx = sfx.rng.integers(0, n, int(density * dur))
    x[idx] = sfx.rng.uniform(-1, 1, len(idx))
    return highpass(x, bright, 1)


def thud(f=60, tau=0.12, dur=0.4, drop=0.6):
    return sine(glide(f, f * drop), dur) * decay(int(dur * SR), tau)


def bell(f0=220, dur=4.0, bright=1.0):
    parts = [(0.5, 1.0, 1.0), (1.0, 0.8, 0.8), (1.19, 0.5, 0.6), (1.56, 0.45, 0.5), (2.0, 0.4, 0.4),
             (2.51, 0.3, 0.3), (2.66, 0.25, 0.25), (3.01, 0.2, 0.2), (4.1, 0.15 * bright, 0.12)]
    n = int(dur * SR)
    out = sum(a * sine(f0 * r, dur) * decay(n, dur * d * 0.5) for r, a, d in parts)
    return out * env(n, 0.002, 0.01, 1, 0.05)


def static_noise(dur, crush=True):
    s = bandpass(noise(dur), 2500, 0.6) * (0.6 + 0.4 * smooth_noise(dur, 30)) + crackle(dur, 600, 1500) * 2
    return bitcrush(s, 5, 3) if crush else s


def step(kind="leaf", heavy=1.0):
    dur = 0.35
    n = int(dur * SR)
    body = lowpass(noise(dur), 600 if kind != "bone" else 1200) * decay(n, 0.04 * heavy)
    th = thud(55 * (1.2 - 0.3 * heavy), 0.06 * heavy, dur) * 0.8 * heavy
    top = {"leaf": crackle(dur, 900, 2500) * decay(n, 0.05) * 3,
           "wet": bandpass(noise(dur), 900, 3) * decay(n, 0.08) * 1.5,
           "bone": crackle(dur, 400, 1800) * decay(n, 0.07) * 5,
           "stone": bandpass(noise(dur), 2500, 2) * decay(n, 0.02) * 2}[kind]
    return fade(body + th + top, 0.001, 0.05)


def steps(count, interval, kind="leaf", heavy=1.0, jitter=0.12, fade_out=False, dist=1.0):
    parts = []
    for i in range(count):
        g = (1 - i / count * 0.7) if fade_out else 1.0
        parts.append((step(kind, heavy) * g * sfx.rng.uniform(0.7, 1.0), i * interval * sfx.rng.uniform(1 - jitter, 1 + jitter)))
    s = mix(*parts)
    return lowpass(s, 4000 / dist) if dist > 1 else s


def click(f=3200, tau=0.004, dur=0.06):
    n = int(dur * SR)
    return (noise(dur) * decay(n, 0.0015) + sine(f, dur) * decay(n, tau) * 0.8 + sine(f * 1.6, dur) * decay(n, tau * 0.6) * 0.4)


def metal_ring(f0=1800, dur=0.6, tau=0.15):
    ratios = [1, 1.47, 2.09, 2.56, 3.17]
    n = int(dur * SR)
    return sum(sine(f0 * r * sfx.rng.uniform(0.98, 1.02), dur) * decay(n, tau / (1 + i * 0.4)) / (1 + i) for i, r in enumerate(ratios))


def tear(dur=0.6):
    n = int(dur * SR)
    gr = np.abs(smooth_noise(dur, 300)) ** 2
    return bandpass(noise(dur), 3500, 1.2) * gr * env(n, 0.02, 0.1, 0.8, 0.15) + crackle(dur, 500, 2000) * 2


def groan(dur=1.0, f0=110, vowels=("o", "uh", "u"), drive=2.0):
    f = vibrato(1, dur, 4, 0.03, jitter=0.05) * np.interp(np.linspace(0, 1, int(dur * SR)), [0, 0.3, 1], [f0, f0 * 1.1, f0 * 0.75])
    src = saw(f, dur) + noise(dur) * 0.2
    return distort(formant_morph(src, list(vowels), q=6) * env(int(dur * SR), 0.05, 0.1, 0.9, 0.3), drive)


def growl(dur=1.5, f0=75):
    n = int(dur * SR)
    am = 0.55 + 0.45 * np.sin(2 * np.pi * (28 + 6 * smooth_noise(dur, 3)) * t(dur))
    src = (saw(vibrato(f0, dur, 3, 0.05, jitter=0.1), dur) + noise(dur, "pink") * 0.6) * am
    v = formant_morph(src, [str(sfx.rng.choice(["uh", "o", "u", "a"])) for _ in range(4)], q=4, shift=0.8)
    return distort(v * env(n, 0.15, 0.2, 0.9, 0.4), 2.5)


def choir(dur, notes=(220, 261.6, 329.6), vowel="o", detune=0.006, voices=4, wet=0.5):
    out = 0
    for f in notes:
        for v in range(voices):
            ff = vibrato(f * (1 + sfx.rng.uniform(-detune, detune)), dur, sfx.rng.uniform(4.5, 6), 0.008)
            out = out + saw(ff, dur) * 0.3
    out = formant(out, vowel, q=5) * env(int(dur * SR), dur * 0.25, 0.1, 1, dur * 0.35)
    return reverb(out, 3.0, wet)


def drone(dur, f0=45, voices=5):
    out = sum(saw(f0 * (1 + sfx.rng.uniform(-0.01, 0.01)) * (1 + 0.005 * smooth_noise(dur, 0.3)), dur) for _ in range(voices))
    return lowpass(out, 220, 2)


def heartbeat_pair(f=50):
    a = thud(f, 0.08, 0.35, 0.7)
    b = thud(f * 0.9, 0.1, 0.4, 0.7) * 0.7
    return lowpass(mix(a, (b, 0.22)), 300)


def glitch_chop(src, pieces=14, crush=True):
    n = len(src)
    out = []
    for _ in range(pieces):
        L = int(sfx.rng.uniform(0.012, 0.05) * SR)
        a = int(sfx.rng.uniform(0, max(1, n - L)))
        seg = src[a:a + L]
        out.extend([seg] * int(sfx.rng.integers(1, 4)))
        if sfx.rng.random() < 0.2: out.append(np.zeros(int(0.015 * SR)))
    s = np.concatenate(out)
    return bitcrush(s, 4, 6) if crush else s


def bubbles(dur, rate=25):
    n = int(dur * SR)
    out = np.zeros(n)
    for _ in range(int(dur * rate)):
        L = int(sfx.rng.uniform(0.015, 0.04) * SR); a = int(sfx.rng.uniform(0, n - L))
        f0 = sfx.rng.uniform(300, 900)
        out[a:a + L] += np.sin(2 * np.pi * np.cumsum(np.linspace(f0, f0 * 1.8, L)) / SR) * decay(L, L / SR / 3) * sfx.rng.uniform(0.3, 1)
    return out


def gulp():
    d = 0.32
    src = saw(glide(170, 120), d) + noise(d) * 0.3
    v = formant(src, "u", q=5) * env(int(d * SR), 0.01, 0.05, 0.7, 0.12)
    return lowpass(v, 1500) + bubbles(d, 20) * 0.15


def wind(dur, strength=1.0):
    center = 500 + 350 * smooth_noise(dur, 0.8)
    base = noise(dur, "pink")
    # filtr pasmowy o zmiennej częstotliwości: kilka pasm z płynnymi wagami
    bands = [250, 450, 700, 1100]
    out = sum(bandpass(base, b, 2.5) * np.clip(1 - np.abs(center - b) / 400, 0, 1) for b in bands)
    howl = sine(vibrato(sfx.rng.uniform(380, 560), dur, 0.4, 0.15, jitter=0.2), dur) * (0.5 + 0.5 * smooth_noise(dur, 0.7)) * 0.15
    return (out + howl) * env(int(dur * SR), dur * 0.35, 0.1, 1, dur * 0.4) * strength


def chirp():
    d = sfx.rng.uniform(0.05, 0.14)
    f0 = sfx.rng.uniform(2500, 4500)
    f = f0 * (1 + 0.35 * np.sin(2 * np.pi * sfx.rng.uniform(25, 45) * t(d))) * np.linspace(1, sfx.rng.uniform(0.8, 1.3), int(d * SR))
    return sine(f, d) * np.hanning(int(d * SR))


def bird_song(dur):
    parts = []
    tt = 0.0
    while tt < dur - 0.3:
        phrase = sfx.rng.integers(2, 6)
        for k in range(phrase):
            parts.append((chirp() * sfx.rng.uniform(0.3, 1), tt)); tt += sfx.rng.uniform(0.07, 0.16)
        tt += sfx.rng.uniform(0.3, 1.0)
    return mix(*parts)


def creak(dur=1.5):
    n = int(dur * SR)
    rate = 60 + 50 * smooth_noise(dur, 2)
    ph = np.cumsum(rate) / SR
    pulses = np.zeros(n); idx = np.nonzero(np.diff(np.floor(ph)) > 0)[0]; pulses[idx] = 1
    res = bandpass(pulses, 420, 6) + bandpass(pulses, 950, 8) * 0.6 + bandpass(pulses, 180, 4) * 0.5
    return res * env(n, 0.2, 0.1, 1, 0.4) * (0.6 + 0.4 * smooth_noise(dur, 3))


def knock_hit():
    d = 0.25
    n = int(d * SR)
    x = noise(d) * decay(n, 0.006)
    return bandpass(x, 320, 5) * 3 + bandpass(x, 850, 8) * 2 + thud(110, 0.03, d) * 0.6


def breath(inhale=1.1, exhale=1.4):
    a = formant(noise(inhale, "pink"), "uh", q=3) * env(int(inhale * SR), inhale * 0.6, 0.1, 0.8, inhale * 0.3)
    b = formant(noise(exhale, "pink"), "o", q=3, shift=0.9) * env(int(exhale * SR), 0.08, 0.2, 0.6, exhale * 0.6)
    return highpass(mix(a, (b, inhale + 0.25)), 200)


def sizzle(dur=0.9):
    n = int(dur * SR)
    return (highpass(noise(dur), 3000) * 0.5 + crackle(dur, 1500, 2500) * 3) * env(n, 0.01, 0.2, 0.6, dur * 0.5)


def glass_break():
    d = 0.8
    n = int(d * SR)
    sh = highpass(noise(d), 2500) * decay(n, 0.05) * 1.5
    tinkles = mix(*[(sine(sfx.rng.uniform(3000, 7000), 0.25) * decay(int(0.25 * SR), 0.04) * sfx.rng.uniform(0.2, 0.6), sfx.rng.uniform(0.02, 0.4)) for _ in range(12)])
    return mix(sh, tinkles, (thud(140, 0.03, 0.2) * 0.6, 0))


def splash(d=0.6):
    n = int(d * SR)
    return lowpass(noise(d), 2500) * env(n, 0.005, 0.1, 0.4, 0.4) + bubbles(d, 40) * 0.4


def thread_grind(dur=0.5):
    n = int(dur * SR)
    rate = 140 + 40 * smooth_noise(dur, 4)
    ph = np.cumsum(rate) / SR
    p = np.zeros(n); p[np.nonzero(np.diff(np.floor(ph)) > 0)[0]] = 1
    return bandpass(p, 2200, 3) * 2 + bandpass(noise(dur), 3000, 2) * 0.1


# =================================================================== przepisy (event -> funkcja(i) -> sygnał)
def r_screamer(i):
    d = 1.1
    sc = scream(d, f0=[620, 760, 540, 880][i], vowels=[("a", "e", "a"), ("e", "i", "e"), ("a", "o", "a"), ("i", "e", "a")][i], rough=0.6, drive=5)
    sc2 = scream(d, f0=[930, 1140, 820, 1300][i], vowels=("a", "e"), rough=0.8, drive=6) * 0.5
    hit = thud(48, 0.25, 1.0, 0.5) * 1.5
    st = static_noise(d) * env(int(d * SR), 0.001, 0.2, 0.3, 0.5) * 0.5
    m = mix(sc, sc2, hit, st)
    m = m * env(len(m), 0.001, 0.05, 1, 0.45)
    return reverb(pan(m, [-0.3, 0.3, -0.2]), 0.8, 0.2)


def r_spooky_buildup(i):
    d = 3.2
    beats, tt, gap = [], 0.0, 0.75
    while tt < d - 0.5:
        beats.append((heartbeat_pair(52), tt)); tt += gap; gap *= 1.18
    hb = mix(*beats)
    wh = whisper(2.4, 9) * 0.5
    wh = reverb(pan(wh, [1, -1]), 1.5, 0.5)
    dr = drone(d, 38) * env(int(d * SR), 1.0, 0.1, 1, 1.0) * 0.6
    return mix(pan(hb, 0), (wh, 0.6), pan(dr, 0))


def r_spooky(i):
    d = 1.6
    sc = scream(d, 520, ("a", "o", "e", "a"), rough=1.0, rise=1.6, drive=8)
    metal = sum(sine(glide(f, f * 0.7), d) for f in (1840, 2710, 3390, 4630)) * 0.25
    st = static_noise(d) * 0.8
    hit = thud(40, 0.4, d, 0.4) * 2
    m = mix(sc, metal, st, hit) * env(int(d * SR), 0.001, 0.05, 1, 0.01)
    m = np.concatenate([m, np.zeros(int(0.05 * SR))])  # ucięcie do ciszy
    return pan(distort(m, 3), [-0.6, 0.6, -0.6, 0.6])


def r_agg_ambient(i):
    return reverb(growl(sfx.rng.uniform(1.4, 2.2), [72, 64, 85][i]), 1.2, 0.2)


def r_agg_chase(i):
    d = 1.4
    n = int(d * SR)
    strings = sum(saw(vibrato(f, d, 9, 0.02), d) for f in ([1480, 1568, 1661] if i == 0 else [1245, 1318, 1397]))
    strings = highpass(strings, 800) * env(n, 0.01, 0.15, 0.7, 0.6) * (0.7 + 0.3 * np.sin(2 * np.pi * 14 * t(d)))
    pant = mix(*[(breath(0.18, 0.22) * 0.8, k * 0.4) for k in range(3)])
    return reverb(mix(strings, (pant, 0.3)), 1.0, 0.25)


def r_agg_hurt(i):
    return mix(tear(0.35) * 0.7, (groan(0.5, [120, 105][i], ("uh", "o"), 2.5), 0.03))


def r_agg_death(i):
    return reverb(mix(tear(0.9), (scream(1.4, 560, ("a", "o", "u"), rise=0.6, drive=3) * 0.7, 0.1)), 1.5, 0.3)


def r_neu_ambient(i):
    d = 1.6
    giggle = mix(*[(formant(saw(glide(f, f * 0.9), 0.09), "ee", q=6) * np.hanning(int(0.09 * SR)), k * 0.13)
                   for k, f in enumerate(np.linspace(sfx.rng.uniform(450, 600), sfx.rng.uniform(350, 420), sfx.rng.integers(4, 8)))])
    if i == 2: giggle = reverse(giggle)
    return lowpass(reverb(giggle, 2.0, 0.55), 3500)


def r_neu_scare(i):
    return mix(scream(0.6, [880, 980, 760][i], ("o", "a"), rough=0.4, rise=1.2, drive=4), thud(70, 0.1, 0.5) * 0.8)


def r_neu_flee(i):
    return steps(7, 0.11, "leaf", 0.6, fade_out=True)


def r_neu_hurt(i):
    d = 0.45
    f = vibrato(1, d, 20, 0.05) * np.linspace([900, 1100][i], [1500, 1700][i], int(d * SR))
    return formant(saw(f, d), "ee", q=4) * env(int(d * SR), 0.005, 0.05, 0.8, 0.15)


def r_neu_death(i):
    pop = noise(0.05) * decay(int(0.05 * SR), 0.005) * 3 + thud(200, 0.02, 0.05)
    return mix(r_neu_hurt(0), (pop, 0.42))


def r_unh_ambient(i):
    d = 3.0
    ch = choir(d, [(110, 116.5, 164.8), (98, 103.8, 146.8), (123.5, 130.8, 185)][i], "u", voices=3, wet=0.4)
    wh = reverse(whisper(d * 0.8, 8)) * 0.6
    sub = drone(d, 41, 3) * 0.5
    return mix(ch, (reverb(wh, 2, 0.5), 0.2), sub)


def r_unh_attack(i):
    d = 0.8
    imp = mix(thud(65, 0.08, 0.4), lowpass(noise(0.1), 1500) * decay(int(0.1 * SR), 0.02))
    suck = reverse(highpass(noise(0.6), 400) * decay(int(0.6 * SR), 0.12)) * 0.6
    return mix(imp, (suck, 0.1), (drone(0.7, 55, 2) * 0.3, 0.1))


def r_unh_hurt(i):
    return mix(sizzle(0.9), (highpass(noise(0.7), 5000) * env(int(0.7 * SR), 0.02, 0.1, 0.6, 0.4) * 0.6, 0.05),
               (groan(0.6, 90, ("o", "u"), 3) * 0.5, 0.05))


def r_unh_immune(i):
    return mix(lowpass(metal_ring(420, 0.7, 0.12), 2500), thud(90, 0.03, 0.3) * 0.7)


def r_unh_death(i):
    voices = [scream(2.0, f, ("a", "o", "u"), rise=0.5, drive=2.5) * 0.4 for f in (420, 560, 690)]
    return reverb(mix(*voices), 4.0, 0.6, damp=2500)


def r_hal_spawn(i):
    d = 1.2
    sw = bandpass(noise(d, "pink"), 900, 1) * env(int(d * SR), 0.9, 0.05, 1, 0.2) * 0.5
    inh = formant(noise(0.8, "pink"), "uh", q=3) * env(int(0.8 * SR), 0.6, 0.1, 0.8, 0.1)
    return mix(sw, (highpass(inh, 300), 0.35))


def r_hal_ambient(i):
    return reverb(whisper(sfx.rng.uniform(1.6, 2.4)), 2.5, 0.6)


def r_hal_hurt(i):
    d = 0.8
    f = vibrato(200, d, 3, 0.15)
    return reverb(lowpass(formant(saw(f, d), "o", q=5), 1200) * env(int(d * SR), 0.05, 0.1, 0.8, 0.3), 1.0, 0.4)


def r_hal_dissolve(i):
    d = 1.5
    tone = sine(glide(600, 150), d) * decay(int(d * SR), 0.5) * 0.5
    dust = highpass(noise(d), 2000) * env(int(d * SR), 0.01, 0.2, 0.5, 1.0) * 0.4
    return reverb(mix(tone, dust, crackle(d, 150, 2000)), 2, 0.4)


def r_sl_ambient(i):
    d = 4.0
    st = static_noise(d, crush=False) * 0.25 * (0.7 + 0.3 * smooth_noise(d, 0.5))
    hum = sine(60, d) * 0.3 + sine(120, d) * 0.15
    b = lowpass(bell([98, 87][i], 3.5), 1200) * 0.6
    return mix(st, hum, (reverb(b, 3, 0.6), 0.6))


def r_sl_proximity(i):
    d = 8.5
    st = static_noise(d) * 0.6 * (0.8 + 0.2 * smooth_noise(d, 2))
    hum = sine(60, d) * 0.4 + saw(30, d) * 0.15
    return loop_crossfade(lowpass(st + hum, 7000), 0.5)


def r_sl_teleport(i):
    src = mix(static_noise(0.6), sine(glide(400, 80), 0.6) * 0.6, scream(0.6, 300, ("o",), drive=3) * 0.3)
    g = glitch_chop(src, [12, 16, 10][i])
    return mix(g, (crackle(0.1, 3000, 1000) * 2, len(g) / SR - 0.05))


def r_sl_summon(i):
    d = 3.0
    roar = growl(1.6, 52) * 1.2
    ws = sum(reverb(whisper(1.8, 6), 2.5, 0.5) for _ in range(3))
    return mix(roar, (ws * 0.6, 1.0))


def r_sl_hurt(i):
    return mix(static_noise(0.4) * decay(int(0.4 * SR), 0.12) * 1.5, metal_ring([1300, 1650][i], 0.5, 0.08) * 0.8)


def r_sl_death(i):
    d = 3.0
    st = static_noise(d) * np.linspace(1, 0, int(d * SR)) ** 2
    b = reverb(bell(73, 4.0), 4, 0.6) * 0.7
    return mix(st, (b, 2.8))


def r_sl_vanish(i):
    g = glitch_chop(static_noise(0.5), 10)
    rb = reverse(reverb(bell(110, 2.5), 2.5, 0.6)) * 0.6
    return mix(g, (rb, 0.2))


def r_sl_deadly(i):
    d = 2.5
    roar = mix(growl(d, 46) * 1.2, scream(d, 260, ("o", "a", "o"), rough=1.0, rise=1.2, drive=6) * 0.6)
    return reverb(distort(roar, 3), 2.0, 0.3)


def r_low_sanity(i):
    if i == 0: return steps(4, 0.55, "stone", 0.9, dist=1.5)
    if i == 1: return reverb(mix(*[(knock_hit(), k * 0.32) for k in range(3)]), 1.2, 0.3)
    if i == 2:
        d = 1.8
        return bandpass(noise(d), 1800, 1.5) * np.abs(np.sin(2 * np.pi * 3.5 * t(d))) ** 3 * env(int(d * SR), 0.1, 0.1, 1, 0.3)
    return reverb(breath(1.0, 1.3), 0.8, 0.2)


def r_heartbeat(i):
    return np.concatenate([heartbeat_pair(55), np.zeros(int(2.0 * SR) - len(heartbeat_pair(55)))])


def r_dark_corner(i):
    d = 10.0
    dr = drone(d, 38) * 0.6
    insects = sum(sine(sfx.rng.uniform(3000, 6000), d) * (0.5 + 0.5 * np.sin(2 * np.pi * sfx.rng.uniform(30, 80) * t(d))) * (0.5 + 0.5 * smooth_noise(d, 1)) for _ in range(10)) * 0.03
    cr = crackle(d, 40, 1500) * 0.8
    return loop_crossfade(mix(dr, insects, cr), 1.0)


def r_blood_moon(i):
    d = 6.5
    tolls = mix(*[(lowpass(bell(65.4, 4.0), 1500), k * 1.6) for k in range(3)])
    ch = choir(d, (130.8, 155.6, 196), "a", voices=3, wet=0.6) * 0.5
    return reverb(pan(mix(tolls, ch), [-0.3, 0.3]), 3.0, 0.4)


def r_bless(i):
    d = 3.0
    ch = choir(d, (523, 659, 784), "a", voices=3, wet=0.6) * 0.6
    chimes = mix(*[(bell(sfx.rng.uniform(1500, 2600), 1.5), sfx.rng.uniform(0, 1.8)) for _ in range(6)]) * 0.2
    return mix(ch, chimes)


def r_holy_sizzle(i):
    return mix(sizzle(0.8), (highpass(noise(1.2), 1500) * env(int(1.2 * SR), 0.05, 0.3, 0.5, 0.6) * 0.3, 0.1))


def r_crucifix_use(i):
    b = bell([1320, 1480][i], 1.6, 0.5) * 0.7
    ch = choir(1.4, (523, 659, 784) if i == 0 else (587, 740, 880), "a", voices=2, wet=0.5) * 0.5
    return mix(b, (ch, 0.02))


def r_crucifix_save(i):
    crack = mix(tear(0.3) * 0.5, thud(120, 0.04, 0.3), crackle(0.3, 2000, 800) * 2)
    return mix(crack, (choir(2.5, (392, 494, 587), "a", voices=3, wet=0.6) * 0.6, 0.1), (bell(784, 2.5) * 0.5, 0.1))


def r_fl_on(i):
    return mix(click(3000), (sine(120, 0.35) * 0.08 * env(int(0.35 * SR), 0.05, 0.1, 1, 0.2), 0.02))


def r_fl_off(i):
    return click(2200, 0.005)


def r_core_in(i):
    return mix(click(2600, 0.006), (metal_ring(2400, 0.3, 0.05) * 0.5, 0.0), (click(1900, 0.004), 0.09))


def r_core_out(i):
    return mix(click(1900, 0.004), (bandpass(noise(0.2), 2500, 2) * env(int(0.2 * SR), 0.02, 0.05, 0.6, 0.1) * 0.5, 0.05), (click(2500), 0.2))


def r_sword_charge(i):
    d = 1.5
    pulse = 0.5 + 0.5 * np.sin(2 * np.pi * 4 * t(d))
    inh = reverse(highpass(noise(d, "pink"), 300) * decay(int(d * SR), 0.4)) * 0.6
    return mix(drone(d, 55, 3) * pulse, inh, (sine(glide(80, 160), d) * 0.3, 0))


def r_sword_dis(i):
    d = 1.4
    return mix(saw(glide(110, 40), d) * decay(int(d * SR), 0.5) * 0.5, sine(glide(220, 60), d) * decay(int(d * SR), 0.4) * 0.3)


def r_thermos_drink(i):
    return mix(*[(gulp(), k * sfx.rng.uniform(0.38, 0.45)) for k in range(2 + i)])


def r_thermos_fill(i):
    d = 1.4
    return mix(bubbles(d, 60) * 0.6, lowpass(noise(d), 1200) * env(int(d * SR), 0.05, 0.2, 0.5, 0.4) * 0.5) * np.linspace(0.6, 1, int(d * SR))


def r_note_rip(i):
    return tear(sfx.rng.uniform(0.4, 0.7))


def r_cough(i):
    d = 0.22
    one = lambda: formant(noise(d, "pink"), "a", q=2) * env(int(d * SR), 0.002, 0.05, 0.3, 0.1) + thud(120, 0.02, d) * 0.3
    return mix(*[(one(), k * 0.28) for k in range(2 + i)])


def r_whisper(i):
    return whisper(sfx.rng.uniform(0.8, 1.4), 3)


def r_distant_steps(i):
    return reverb(steps(sfx.rng.integers(3, 6), 0.5, "leaf", 0.8, dist=2.5), 1.5, 0.3)


def r_knock(i):
    return reverb(lowpass(mix(*[(knock_hit(), k * sfx.rng.uniform(0.28, 0.4)) for k in range(3 + 2 * i)]), 2500), 1.8, 0.4)


def r_branch(i):
    d = 0.5
    crack = mix(crackle(d, 3000, 1000) * env(int(d * SR), 0.001, 0.08, 0.1, 0.1) * 3, thud(160, 0.02, 0.2) * 0.6)
    return reverb(crack, 1.0, 0.25)


def r_breath(i):
    return breath(sfx.rng.uniform(0.9, 1.3), sfx.rng.uniform(1.2, 1.7))


def r_creak(i):
    return reverb(creak(sfx.rng.uniform(1.2, 2.2)), 1.0, 0.2)


def r_birds(i):
    return reverb(bird_song(5.0), 1.2, 0.2)


def r_birds_silence(i):
    d = 0.9
    flaps = mix(*[(bandpass(noise(0.06), 700, 1.5) * np.hanning(int(0.06 * SR)) * sfx.rng.uniform(0.4, 1), sfx.rng.uniform(0, 0.8)) for _ in range(25)])
    ring_ = sine(7800, 2.5) * env(int(2.5 * SR), 0.6, 0.2, 0.5, 1.2) * 0.04
    return mix(flaps, (np.zeros(int(0.4 * SR)), d), (ring_, d + 0.2))


def r_wind(i):
    return wind(sfx.rng.uniform(4, 6))


def r_fake_steps(i):
    return steps(sfx.rng.integers(3, 7), 0.48, ["leaf", "stone", "leaf", "wet"][i], 0.9)


def r_wall_scream(i):
    sc = scream(1.2, [640, 720, 560][i], ("a", "e", "a"), rough=0.5, drive=3)
    return reverb(lowpass(sc, 600, 3), 1.2, 0.35)


def r_slop_step(i):
    return step("wet", 0.8) * 0.9


def r_unholy_step(i):
    return step("bone", 1.3)


def r_float(i):
    d = 2.2
    fl = bandpass(noise(d, "pink"), 600, 1.5) * (0.6 + 0.4 * np.sin(2 * np.pi * 1.2 * t(d))) * 0.6
    return loop_crossfade(fl, 0.3)


def r_slender_step(i):
    return reverb(step("leaf", 1.6), 1.4, 0.35)


def r_thermos_open(i):
    return mix(thread_grind(0.45), (mix(click(1400, 0.01) * 0.8, thud(300, 0.01, 0.08) * 0.5), 0.45))


def r_thermos_close(i):
    return mix(thread_grind(0.4), (mix(thud(180, 0.02, 0.15), click(1200, 0.006) * 0.6), 0.4))


def r_crucifix_hit(i):
    return mix(knock_hit() * 1.2, (bell(1760 if i == 0 else 1975, 0.8, 0.4) * 0.35, 0.01))


def r_holy_shatter(i):
    return mix(glass_break(), (splash(0.6) * 0.6, 0.02), (choir(1.0, (659, 784, 988), "a", voices=2, wet=0.5) * 0.25, 0.05))


def r_sword_hit(i):
    d = 0.45
    slash = bandpass(noise(d), 1800, 1.2) * env(int(d * SR), 0.002, 0.05, 0.3, 0.2)
    flesh = lowpass(noise(d), 700) * decay(int(d * SR), 0.05) * 1.2
    puls = distort(sine(glide([70, 60, 80][i], 35), d) * decay(int(d * SR), 0.15), 3) * 0.6
    return mix(slash, flesh, puls)


def eleven(i):
    src = os.environ.get("ELEVEN") or str(ROOT / "assets_src/audio_src/11.ogg")
    if not Path(src).exists():
        raise FileNotFoundError(f"Brak płyty 11: {src}")
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", src, "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    x = np.frombuffer(raw, np.float32).astype(float)
    x = pitch(reverse(x), 0.6)                                   # odwrócona i spowolniona (niżej)
    st = np.stack([x, np.concatenate([np.zeros(int(0.018 * SR)), x[:-int(0.018 * SR)]])], 1)  # szerokość stereo
    st = lowpass(st, 5000)
    st = reverb(st, 3.5, 0.35, damp=3000)[:len(x)]
    return loop_crossfade(st, 3.0)


RECIPES = {
    "jumpscare.screamer": r_screamer, "jumpscare.spooky_buildup": r_spooky_buildup, "jumpscare.spooky": r_spooky,
    "entity.slop.aggressive.ambient": r_agg_ambient, "entity.slop.aggressive.chase": r_agg_chase,
    "entity.slop.aggressive.hurt": r_agg_hurt, "entity.slop.aggressive.death": r_agg_death,
    "entity.slop.neutral.ambient": r_neu_ambient, "entity.slop.neutral.scare": r_neu_scare,
    "entity.slop.neutral.flee": r_neu_flee, "entity.slop.neutral.hurt": r_neu_hurt, "entity.slop.neutral.death": r_neu_death,
    "entity.slop.unholy.ambient": r_unh_ambient, "entity.slop.unholy.attack": r_unh_attack,
    "entity.slop.unholy.hurt": r_unh_hurt, "entity.slop.unholy.immune": r_unh_immune, "entity.slop.unholy.death": r_unh_death,
    "entity.hallucination.spawn": r_hal_spawn, "entity.hallucination.ambient": r_hal_ambient,
    "entity.hallucination.hurt": r_hal_hurt, "entity.hallucination.dissolve": r_hal_dissolve,
    "entity.slenderman.ambient": r_sl_ambient, "entity.slenderman.proximity": r_sl_proximity,
    "entity.slenderman.teleport": r_sl_teleport, "entity.slenderman.summon": r_sl_summon,
    "entity.slenderman.hurt": r_sl_hurt, "entity.slenderman.death": r_sl_death, "entity.slenderman.vanish": r_sl_vanish,
    "entity.slenderman.deadly_roar": r_sl_deadly,
    "music.eleven_reversed": eleven,
    "ambient.low_sanity": r_low_sanity, "ambient.heartbeat": r_heartbeat, "ambient.dark_corner": r_dark_corner,
    "ambient.blood_moon.rise": r_blood_moon, "ambient.full_moon.bless": r_bless,
    "item.holy_water.sizzle": r_holy_sizzle, "item.crucifix.use": r_crucifix_use, "item.crucifix.save": r_crucifix_save,
    "item.flashlight.toggle_on": r_fl_on, "item.flashlight.toggle_off": r_fl_off,
    "item.flashlight.insert_core": r_core_in, "item.flashlight.remove_core": r_core_out,
    "item.unholy_sword.charge": r_sword_charge, "item.unholy_sword.discharged": r_sword_dis,
    "item.thermos.drink": r_thermos_drink, "item.thermos.fill": r_thermos_fill,
    "block.tree_note.rip": r_note_rip, "player.dehydrated": r_cough,
    "ambient.forest.whisper": r_whisper, "ambient.forest.distant_steps": r_distant_steps, "ambient.forest.knock": r_knock,
    "ambient.forest.branch_snap": r_branch, "ambient.forest.breath": r_breath, "ambient.forest.tree_creak": r_creak,
    "ambient.forest.birds": r_birds, "ambient.forest.birds_silence": r_birds_silence, "ambient.wind": r_wind,
    "ambient.sanity.fake_steps": r_fake_steps, "ambient.sanity.wall_scream": r_wall_scream,
    "entity.slop.step": r_slop_step, "entity.slop.unholy.step": r_unholy_step, "entity.slop.float": r_float,
    "entity.slenderman.step": r_slender_step,
    "item.thermos.open": r_thermos_open, "item.thermos.close": r_thermos_close, "item.crucifix.hit": r_crucifix_hit,
    "item.holy_water.shatter": r_holy_shatter, "item.unholy_sword.hit": r_sword_hit,
}
LOOPS = {"music.eleven_reversed", "entity.slenderman.proximity", "ambient.dark_corner", "ambient.heartbeat", "entity.slop.float"}
STEREO = {"music.eleven_reversed", "jumpscare.screamer", "jumpscare.spooky_buildup", "jumpscare.spooky", "ambient.blood_moon.rise"}
LOUD = {"jumpscare.screamer": -12, "jumpscare.spooky": -11, "entity.slop.neutral.scare": -14, "entity.slenderman.deadly_roar": -14,
        "music.eleven_reversed": -20, "entity.slenderman.proximity": -20, "ambient.dark_corner": -22, "ambient.forest.breath": -22,
        "ambient.forest.whisper": -21, "ambient.heartbeat": -16, "item.flashlight.toggle_off": -20, "item.flashlight.toggle_on": -20}


def trim(sig, thr_db=-55):
    """Obcina ciszę na początku i długie, niesłyszalne ogony pogłosu."""
    mono = np.abs(sig if sig.ndim == 1 else sig.mean(1))
    thr = mono.max() * 10 ** (thr_db / 20)
    idx = np.nonzero(mono > thr)[0]
    if not len(idx): return sig
    a = max(0, idx[0] - int(0.003 * SR)); b = min(len(sig), idx[-1] + int(0.05 * SR))
    return sig[a:b]


def main():
    flt = sys.argv[1] if len(sys.argv) > 1 else ""
    missing = [s[0] for s in SOUNDS if s[0] not in RECIPES]
    if missing: print("BRAK PRZEPISU:", missing)
    for sid, n, stream, *_ in SOUNDS:
        if flt not in sid or sid not in RECIPES: continue
        base = sid.replace(".", "/")
        for i in range(n):
            sfx.seed(zlib.crc32(f"{sid}:{i}".encode()))
            try:
                sig = RECIPES[sid](i)
            except Exception as e:
                import traceback; print("BŁĄD", sid, i, repr(e), traceback.format_exc().splitlines()[-3]); continue
            name = f"{base}{i + 1}" if n > 1 else base
            st = sid in STEREO
            sig = sig if st or sig.ndim == 1 else sig.mean(1)
            if sid not in LOOPS: sig = fade(trim(sig), 0.002, 0.02)
            write_ogg(sig, OUT / f"{name}.ogg", quality=3 if stream else 5, rms_db=LOUD.get(sid, -18), peak_db=-3.0, stereo=st)
            print("ok", name, f"{len(sig) / SR:.1f}s")


if __name__ == "__main__":
    main()
