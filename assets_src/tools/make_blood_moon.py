"""Krwawy Księżyc: tekstura nieba 32x32 w formacie vanilla (environment/celestial/moon/*.png):
nieprzezroczysta, czarne tło = brak światła (blending addytywny), tarcza 8x8 w środku + łuna."""
import math
from pathlib import Path
from PIL import Image
from sprites import hexc

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/unholymc/textures/environment/celestial/blood_moon.png"
PREV = ROOT / "assets_src/preview/blood_moon_preview.png"

MOON = """
aabbbbaa
abcbbdba
bbbcddbb
bdbbbbcb
bbdcbbbb
bcbbdbdb
abbbbbba
aaabbaaa"""
PAL = {"a": hexc("8a1a14"), "b": hexc("c8281c"), "c": hexc("ff5a3a"), "d": hexc("6a0c0a")}

img = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
# łuna: szersza i mocniejsza niż w vanilla, ciemnoczerwona
for y in range(32):
    for x in range(32):
        d = math.hypot(x - 15.5, y - 15.5)
        k = max(0.0, 1 - d / 16.5) ** 1.8
        img.putpixel((x, y), (int(150 * k), int(10 * k), int(6 * k), 255))
rows = MOON.strip("\n").split("\n")
for y, r in enumerate(rows):
    for x, ch in enumerate(r):
        img.putpixel((12 + x, 12 + y), PAL[ch])
OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT, optimize=True)

# podgląd: vanilla-like niebo nocne vs. niebo Krwawego Księżyca (blending addytywny)
def sky(col):
    s = Image.new("RGB", (32, 32), col)
    a, b = s.load(), img.load()
    for y in range(32):
        for x in range(32):
            a[x, y] = tuple(min(255, a[x, y][i] + b[x, y][i]) for i in range(3))
    return s.resize((256, 256), Image.NEAREST)
pv = Image.new("RGB", (520, 256)); pv.paste(sky((8, 10, 30)), (0, 0)); pv.paste(sky((40, 6, 8)), (264, 0)); pv.save(PREV)
