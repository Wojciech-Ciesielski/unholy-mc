"""Klatki jumpscare'ów jako glify czcionki (dla klienta vanilla, tytuł).

Wynik:
  textures/font/jumpscare/screamer_<1-4>_<0-5>.png   – 4 twarze × 6 klatek, 248×248
  textures/font/jumpscare/spooky_<0-7>.png           – Spooky Jumpscare, 8 klatek, 248×248
  textures/font/jumpscare/static_<0-3>.png           – nakładka szumu (półprzezroczysta), 248×248
  textures/font/jumpscare/backdrop.png / backdrop_red.png – 8×8, rozciągane na cały ekran
  font/jumpscare_frames.json                         – provider czcionki (znaki opisane w docs/ASSETS.md §5)
"""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageOps, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "assets_src/slop_png_raw"
A = ROOT / "src/main/resources/assets/unholymc"
TEX = A / "textures/font/jumpscare"
PREV = ROOT / "assets_src/preview"
S = 248          # rozmiar klatki (limit atlasu czcionki)
W = 512          # rozdzielczość robocza
rng = np.random.default_rng(66)

# źródła twarzy: (plik, kadr, paleta duotonu)
FACES = {
    1: ("the_locust.jfif", (300, 40, 470, 210)),
    2: ("love_note.jfif", (70, 150, 390, 470)),
    3: ("pobierz_2.jfif", (140, 30, 280, 170)),
    4: ("pobierz_4.jfif", (325, 5, 425, 105)),
}
STOPS = [(0.0, (0, 0, 0)), (0.3, (35, 0, 3)), (0.55, (125, 8, 14)), (0.82, (215, 192, 172)), (1.0, (252, 244, 232))]


def duotone(g):
    rgb = np.zeros(g.shape + (3,))
    for (a, ca), (b, cb) in zip(STOPS, STOPS[1:]):
        m = (g >= a) & (g <= b)
        t = ((g - a) / (b - a))[m][:, None]
        rgb[m] = np.array(ca) * (1 - t) + np.array(cb) * t
    return rgb


def prep_face(src, box):
    im = Image.open(RAW / src).convert("L").crop(box).resize((W, W), Image.LANCZOS)
    raw = np.asarray(im).astype(float) / 255
    # jasne tło spójne z krawędzią -> czerń (twarz „wychodzi z ciemności”)
    m = Image.fromarray(np.where(raw > 0.86, 255, 0).astype(np.uint8)).copy()
    for k in range(0, W, 4):
        for pt in ((k, 0), (0, k), (W - 1, k), (k, W - 1)):
            if m.getpixel(pt) == 255: ImageDraw.floodfill(m, pt, 128)
    g = np.asarray(ImageOps.autocontrast(im, cutoff=1)).astype(float) / 255
    g = np.where(np.asarray(m) == 128, 0.0, g)
    # oczy: najjaśniejsze punkty w ciemnych twarzach zostają białe (świecące)
    return g


def vignette(rgb, strength=1.25):
    yy, xx = np.mgrid[0:rgb.shape[0], 0:rgb.shape[1]]
    d = np.hypot(xx - rgb.shape[1] / 2, yy - rgb.shape[0] / 2) / (rgb.shape[0] / 2)
    return rgb * np.clip(strength - d, 0, 1)[..., None]


def transform(g, scale, dx, dy, rot):
    im = Image.fromarray((g * 255).astype(np.uint8))
    im = im.rotate(rot, resample=Image.BICUBIC, fillcolor=0)
    size = int(W * scale)
    im = im.resize((size, size), Image.BICUBIC)
    out = Image.new("L", (W, W), 0)
    out.paste(im, ((W - size) // 2 + dx, (W - size) // 2 + dy))
    return np.asarray(out).astype(float) / 255


def frame(g, scale=1.0, dx=0, dy=0, rot=0, chroma=0, invert=False, static=0.0, red=0.0, blur=0.0):
    gg = transform(g, scale, dx, dy, rot)
    if blur: gg = np.asarray(Image.fromarray((gg * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(blur))).astype(float) / 255
    rgb = duotone(gg)
    if chroma:  # rozszczepienie kanałów
        rgb[..., 0] = np.roll(rgb[..., 0], chroma, axis=1)
        rgb[..., 2] = np.roll(rgb[..., 2], -chroma, axis=1)
        rgb[..., 1] = np.roll(rgb[..., 1], chroma // 2, axis=0)
    if invert: rgb = 255 - rgb
    if red: rgb = rgb * (1 - red) + np.array([150, 0, 0]) * red * (rgb.mean(-1, keepdims=True) / 255 + 0.3)
    if static:
        n = rng.integers(0, 256, rgb.shape[:2])[..., None].astype(float)
        bands = (rng.random(W) < 0.06)[:, None, None]
        rgb = rgb * (1 - static) + n * static
        rgb = np.where(bands, np.roll(rgb, int(rng.integers(-30, 30)), axis=1), rgb)
    rgb += rng.normal(0, 10, rgb.shape)
    rgb = vignette(np.clip(rgb, 0, 255), 1.05 if invert else 1.25)
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8)).resize((S, S), Image.LANCZOS).convert("RGBA")


SCREAMER = [  # 6 klatek ~ 2 ticki każda: wejście, najazd z drżeniem, rozszczepienie, błysk inwersji, krwawe zbliżenie, rozpad
    dict(scale=0.9, static=0.3),
    dict(scale=1.1, dx=10, dy=-4, rot=3, chroma=6),
    dict(scale=1.3, dx=-14, dy=8, rot=-4, chroma=12, static=0.2),
    dict(scale=1.5, dx=6, rot=2, chroma=8, invert=True),
    dict(scale=1.8, dx=-8, dy=-10, rot=-5, chroma=16, red=0.5, static=0.25),
    dict(scale=2.2, blur=6, static=0.7, red=0.3),
]
SPOOKY = [  # 8 klatek: twarz wyłania się z ciemności, potem szał
    dict(scale=0.8, blur=3, static=0.15),
    dict(scale=0.95, static=0.1),
    dict(scale=1.1, dx=12, rot=4, chroma=8),
    dict(scale=1.2, dx=-16, dy=6, rot=-6, chroma=14, invert=True),
    dict(scale=1.35, dx=10, dy=-8, rot=5, chroma=10, red=0.4),
    dict(scale=1.5, dx=-6, rot=-3, chroma=20, invert=True, static=0.2),
    dict(scale=1.8, dx=4, dy=10, rot=7, chroma=24, red=0.6, static=0.3),
    dict(scale=2.4, blur=8, static=0.85, red=0.5),
]


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    sheet_rows = []
    for k, (src, box) in FACES.items():
        g = prep_face(src, box)
        row = []
        for i, p in enumerate(SCREAMER):
            f = frame(g, **p); f.save(TEX / f"screamer_{k}_{i}.png", optimize=True); row.append(f)
        sheet_rows.append(row)
    # Spooky: twarz z gotowej tekstury GUI (the_hollow_screamer, przerobiona wcześniej)
    g = prep_face("the_hollow_screamer.jfif", (195, 15, 395, 215))
    h = g.shape[0]
    yy = np.arange(h)
    sy = np.where(yy > h * 0.55, h * 0.55 + (yy - h * 0.55) * 0.72, yy).astype(int)   # rozciągnięta paszcza
    g = g[np.clip(sy, 0, h - 1)]
    row = []
    for i, p in enumerate(SPOOKY):
        f = frame(g, **p); f.save(TEX / f"spooky_{i}.png", optimize=True); row.append(f)
    sheet_rows.append(row)
    # szum: półprzezroczyste klatki do nałożenia na twarz
    for i in range(4):
        v = rng.integers(0, 256, (S, S))
        a = np.full((S, S), 90)
        a[rng.random(S) < 0.08] = 170
        Image.fromarray(np.dstack([v, v, v, a]).astype(np.uint8)).save(TEX / f"static_{i}.png", optimize=True)
    Image.new("RGBA", (8, 8), (0, 0, 0, 255)).save(TEX / "backdrop.png")
    Image.new("RGBA", (8, 8), (40, 0, 2, 255)).save(TEX / "backdrop_red.png")

    # provider czcionki (własny plik; znaki od U+E300)
    FACE_H, FACE_ASC = 96, 52
    prov, ch = [], 0xE300
    def bitmap(file, h, asc, c):
        prov.append({"type": "bitmap", "file": f"unholymc:font/jumpscare/{file}.png", "height": h, "ascent": asc, "chars": [chr(c)]})
    bitmap("backdrop", 256, 128, 0xE300)
    bitmap("backdrop_red", 256, 128, 0xE301)
    for k in range(1, 5):
        for i in range(6): bitmap(f"screamer_{k}_{i}", FACE_H, FACE_ASC, 0xE310 + (k - 1) * 8 + i)
    for i in range(8): bitmap(f"spooky_{i}", FACE_H, FACE_ASC, 0xE330 + i)
    for i in range(4): bitmap(f"static_{i}", FACE_H, FACE_ASC, 0xE340 + i)
    prov.append({"type": "space", "advances": {chr(0xE3F0): -257, chr(0xE3F1): -97, chr(0xE3F2): 80, chr(0xE3F3): 79}})
    (A / "font").mkdir(exist_ok=True)
    (A / "font/jumpscare_frames.json").write_text(json.dumps({"providers": prov}, indent=1, ensure_ascii=True) + "\n")

    # podgląd
    sh = Image.new("RGBA", (S // 2 * 8, S // 2 * len(sheet_rows)), (0, 0, 0, 255))
    for r, row in enumerate(sheet_rows):
        for c, f in enumerate(row): sh.paste(f.resize((S // 2, S // 2)), (c * S // 2, r * S // 2))
    sh.save(PREV / "jumpscare_frames_preview.png")


if __name__ == "__main__":
    main()
