"""Tekstura modelu 3D Slendermana (64x64, box UV) + 4 klatki statyki na teleport.
Geometria (dla wątku implementacji) jest opisana w docs/ASSETS.md, sekcja Slenderman."""
import random
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/unholymc/textures/entity/slenderman"
PREV = ROOT / "assets_src/preview"
rnd = random.Random(13)

SKIN, SKIN_D, SKIN_L = (196, 192, 184), (150, 146, 140), (222, 218, 210)
BLACK, BLACK_L, BLACK_D = (22, 19, 22), (44, 39, 44), (10, 8, 10)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

def box(u, v, w, h, dd, base, shade):
    """Wypełnia pełny box UV (top, bottom, 4 boki) kolorem bazowym z brudem."""
    regions = [(u + dd, v, w, dd), (u + dd + w, v, w, dd), (u, v + dd, dd, h), (u + dd, v + dd, w, h),
               (u + dd + w, v + dd, dd, h), (u + 2 * dd + w, v + dd, w, h)]
    for (x, y, ww, hh) in regions:
        for yy in range(y, y + hh):
            for xx in range(x, x + ww):
                c = base if rnd.random() > 0.18 else shade
                img.putpixel((xx, yy), c + (255,))
    return {"top": (u + dd, v), "bottom": (u + dd + w, v), "right": (u, v + dd), "front": (u + dd, v + dd),
            "left": (u + dd + w, v + dd), "back": (u + 2 * dd + w, v + dd)}

def px(p, x, y, c): img.putpixel((p[0] + x, p[1] + y), c + (255,))

# głowa 8x10x8 @ (0,0)
H = box(0, 0, 8, 10, 8, SKIN, SKIN_D)
f = H["front"]
# twarz bez rysów: gładka, blada, tylko ledwo widoczne zapadnięte oczodoły i cień pod kośćmi policzkowymi
for y in range(10):
    for x in range(8): px(f, x, y, SKIN)
for x, y in [(1, 4), (2, 4), (5, 4), (6, 4)]: px(f, x, y, (172, 168, 162))
for x, y in [(1, 3), (2, 3), (5, 3), (6, 3)]: px(f, x, y, (186, 182, 175))
for x in range(8): px(f, x, 0, SKIN_L); px(f, x, 9, SKIN_D)
# szyja/ciało 8x14x4 @ (32,0) – wychudzony tors z żebrami
B = box(32, 0, 8, 14, 4, BLACK, BLACK_D)
for y in (3, 5, 7, 9):
    for x in range(1, 7):
        if x not in (3, 4): px(B["front"], x, y, BLACK_L)
for y in range(1, 13): px(B["back"], 3, y, BLACK_L) if y % 2 else None                              # kręgosłup
# ręce 2x30x2 @ (0,18) i (8,18); nogi 2x28x2 @ (16,18) i (24,18)
for u in (0, 8):
    A = box(u, 18, 2, 30, 2, BLACK, BLACK_D)
    for side in ("front", "left", "right", "back"):
        for x in range(2): px(A[side], x, 29, (90, 86, 82))            # pazury
        px(A[side], 0, 14, BLACK_L)                                      # łokieć
for u in (16, 24):
    L = box(u, 18, 2, 28, 2, BLACK, BLACK_D)
    for side in ("front", "left", "right", "back"): px(L[side], 0, 13, BLACK_L)   # kolano
OUT.mkdir(parents=True, exist_ok=True)
img.save(OUT / "slenderman.png", optimize=True)

# teleport: 4 klatki czarno-białej statyki w kształcie modelu (kod podmienia teksturę co tick)
a = np.asarray(img)
statics = []
for i in range(4):
    r = np.random.default_rng(100 + i)
    v = r.integers(0, 256, (64, 64))
    v = np.where(r.random((64, 64)) < 0.5, v // 4, v)            # więcej czerni niż bieli
    al = a[..., 3].copy()
    for y in range(64):                                             # wypadające poziome pasy
        if r.random() < 0.15: al[y] = 0
    st = np.dstack([v, v, v, al]).astype(np.uint8)
    Image.fromarray(st).save(OUT / f"slenderman_static_{i}.png", optimize=True)
    statics.append(st)
g = statics[0]
# złożony front postaci (ortho), skala 4
def face(im, x, y, w, h): return im.crop((x, y, x + w, y + h))
def front(im):
    fig = Image.new("RGBA", (16, 54))
    fig.alpha_composite(face(im, 8, 8, 8, 10), (4, 0))      # głowa
    fig.alpha_composite(face(im, 36, 4, 8, 14), (4, 10))    # tors
    fig.alpha_composite(face(im, 2, 20, 2, 30), (2, 11))    # prawa ręka
    fig.alpha_composite(face(im, 10, 20, 2, 30), (12, 11))  # lewa ręka
    fig.alpha_composite(face(im, 18, 20, 2, 28), (5, 24))   # nogi
    fig.alpha_composite(face(im, 26, 20, 2, 28), (9, 24))
    return fig.resize((64, 216), Image.NEAREST)
pv = Image.new("RGBA", (128 * 2 + 8 + 160, 216), (60, 60, 66, 255))
pv.alpha_composite(front(img), (272, 0)); pv.alpha_composite(front(Image.fromarray(g)), (348, 0))
pv.alpha_composite(img.resize((128, 128), Image.NEAREST), (0, 0))
pv.alpha_composite(Image.fromarray(g).resize((128, 128), Image.NEAREST), (136, 0))
pv.save(PREV / "slenderman_uv_preview.png")
