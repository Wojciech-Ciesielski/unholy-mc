"""Kontrola wygenerowanych .ogg: długość, kanały, szczyt, RMS, cisza na początku, szew pętli. Zapisuje też arkusz spektrogramów."""
import subprocess, sys
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SND = ROOT / "src/main/resources/assets/unholymc/sounds"
LOOPS = {"music/eleven_reversed", "entity/slenderman/proximity", "ambient/dark_corner", "ambient/heartbeat", "entity/slop/float"}

def load(p):
    info = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=channels", "-of", "csv=p=0", str(p)], capture_output=True, text=True).stdout.strip()
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(p), "-f", "f32le", "-ac", "1", "-ar", "22050", "-"], capture_output=True).stdout
    return int(info.split(",")[0]), np.frombuffer(raw, np.float32)

def spec(x, w=240, h=60):
    n = 512; hop = max(1, (len(x) - n) // w)
    cols = []
    for i in range(w):
        seg = x[i * hop:i * hop + n]
        if len(seg) < n: seg = np.pad(seg, (0, n - len(seg)))
        m = np.abs(np.fft.rfft(seg * np.hanning(n)))[:256]
        cols.append(20 * np.log10(m + 1e-6))
    a = np.array(cols).T[::-1]
    a = np.clip((a + 60) / 70, 0, 1)
    img = Image.fromarray((a * 255).astype(np.uint8)).resize((w, h))
    return img

rows, problems = [], []
for p in sorted(SND.rglob("*.ogg")):
    rel = str(p.relative_to(SND))[:-4]
    ch, x = load(p)
    dur = len(x) / 22050; peak = np.abs(x).max(); rms = 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-9)
    lead = np.argmax(np.abs(x) > 0.01) / 22050
    seam = ""
    if rel in LOOPS:
        j = np.abs(x[-1] - x[0]); seam = f" seam={j:.3f}"
        if j > 0.15: problems.append(f"{rel}: skok na szwie pętli {j:.2f}")
    if peak > 0.999: problems.append(f"{rel}: clipping")
    if lead > 0.15: problems.append(f"{rel}: cisza na początku {lead:.2f}s")
    if rms < -40: problems.append(f"{rel}: bardzo cicho {rms:.0f} dB")
    rows.append((rel, ch, dur, peak, rms, lead, seam, x))
for r in rows: print(f"{r[0]:45s} ch={r[1]} {r[2]:6.2f}s peak={r[3]:.2f} rms={r[4]:6.1f}dB lead={r[5]:.2f}{r[6]}")
print("\nPROBLEMY:" if problems else "\nBez problemów.", *problems, sep="\n")
if len(sys.argv) > 1:
    sel = [r for r in rows if sys.argv[1] in r[0]]
    sheet = Image.new("L", (240 * 2 + 10, (len(sel) + 1) // 2 * 72), 0)
    for k, r in enumerate(sel):
        sheet.paste(spec(r[7]), ((k % 2) * 250, (k // 2) * 72))
    sheet.save(sys.argv[2])
