"""Tekstury jumpscare'ów: twarz Spooky Jumpscare, szum (static, animowany), winieta krwi."""
import json, math, random
from pathlib import Path
from PIL import Image, ImageOps, ImageFilter
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc/textures/gui"
RAW = ROOT / "assets_src/slop_png_raw"
PREV = ROOT / "assets_src/preview"

def save(img, rel, mcmeta=None):
    p = A / rel; p.parent.mkdir(parents=True, exist_ok=True); img.save(p, optimize=True)
    if mcmeta: (p.parent / (p.name + ".mcmeta")).write_text(json.dumps(mcmeta, indent=2) + "\n")
    return img

# --- twarz Spooky Jumpscare (z the_hollow_screamer), 512x512, krawędzie wtapiają się w czerń
src = Image.open(RAW / "the_hollow_screamer.jfif").convert("L").crop((190, 15, 400, 225)).resize((512, 512), Image.LANCZOS)
raw = np.asarray(src).astype(float) / 255
g = np.asarray(ImageOps.autocontrast(src, cutoff=2)).astype(float) / 255
# białe tło (spójne z krawędzią) -> czerń
bgm = Image.fromarray(np.where(raw > 0.93, 255, 0).astype(np.uint8)).copy()
from PIL import ImageDraw
for sx0 in range(0, 512, 4):
    for pt in ((sx0, 0), (511, sx0), (0, sx0)):
        if bgm.getpixel(pt) == 255: ImageDraw.floodfill(bgm, pt, 128)
g = np.where(np.asarray(bgm) == 128, 0.0, g)
h, w = g.shape
yy, xx = np.mgrid[0:h, 0:w]
# zniekształcenie: rozciągnięcie dolnej połowy (otwarta paszcza) + lekka fala
sy = np.where(yy > h * 0.55, h * 0.55 + (yy - h * 0.55) * 0.72, yy)
sx = xx + 6 * np.sin(yy / 23.0)
g = g[np.clip(sy, 0, h - 1).astype(int), np.clip(sx, 0, w - 1).astype(int)]
# duoton: czerń -> krwista czerwień -> trupia biel
stops = [(0.0, (0, 0, 0)), (0.35, (40, 0, 2)), (0.6, (130, 10, 14)), (0.85, (214, 190, 170)), (1.0, (250, 240, 228))]
rgb = np.zeros((h, w, 3))
for (a, ca), (b, cb) in zip(stops, stops[1:]):
    m = (g >= a) & (g <= b); t = ((g - a) / (b - a))[m][:, None]
    rgb[m] = np.array(ca) * (1 - t) + np.array(cb) * t
rng = np.random.default_rng(7)
rgb += rng.normal(0, 14, rgb.shape)                      # ziarno
d = np.hypot(xx - w / 2, yy - h / 2) / (w / 2)
rgb *= np.clip(1.25 - d, 0, 1)[..., None]               # winieta do czerni
save(Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8)).convert("RGBA"), "jumpscare/spooky_face.png")

# --- szum TV (sprite GUI, animowany 8 klatek 128x128, półprzezroczysty)
frames = []
for i in range(8):
    n = rng.integers(0, 256, (128, 128))
    band = (np.sin(np.arange(128) / 3.0 + i) > 0.85)[:, None] * 60   # poziome pasy
    v = np.clip(n + band, 0, 255).astype(np.uint8)
    a = np.full_like(v, 110)
    frames.append(np.dstack([v, v, v, a]))
save(Image.fromarray(np.concatenate(frames, 0)), "sprites/jumpscare/static.png", {"animation": {"frametime": 1}})

# --- winieta krwi 256x256 (rozciągana na cały ekran)
yy, xx = np.mgrid[0:256, 0:256]
d = np.hypot(xx - 128, yy - 128) / 181
noise = Image.fromarray(rng.integers(0, 255, (32, 32)).astype(np.uint8)).resize((256, 256), Image.BICUBIC)
nz = np.asarray(noise.filter(ImageFilter.GaussianBlur(6))).astype(float) / 255
alpha = np.clip((d - 0.45 + (nz - 0.5) * 0.35) * 2.6, 0, 1) * 235
vig = np.dstack([np.full_like(d, 90), np.full_like(d, 0), np.full_like(d, 4), alpha]).astype(np.uint8)
save(Image.fromarray(vig), "jumpscare/blood_vignette.png")

# podgląd
pv = Image.new("RGBA", (768, 256), (0, 0, 0, 255))
pv.paste(Image.open(A / "jumpscare/spooky_face.png").resize((256, 256)), (0, 0))
pv.alpha_composite(Image.fromarray(frames[0]).resize((256, 256), Image.NEAREST), (256, 0))
bg = Image.new("RGBA", (256, 256), (120, 120, 120, 255)); bg.alpha_composite(Image.fromarray(vig))
pv.paste(bg, (512, 0)); pv.save(PREV / "jumpscare_preview.png")
