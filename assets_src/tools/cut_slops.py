"""Wycina tło z surowych obrazków slopów i zapisuje przezroczyste PNG + warianty halucynacji.

Użycie (z katalogu repo):  python3 assets_src/tools/cut_slops.py
Wymaga: Pillow, numpy.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "assets_src/slop_png_raw"
OUT = ROOT / "src/main/resources/assets/unholymc/textures/entity/slop"
PREVIEW = ROOT / "assets_src/preview"

CANVAS = 512          # kwadratowe płótno, treść wyrównana do dołu i wyśrodkowana
HALLU_ALPHA = 0.6     # krycie wariantu halucynacji

# nazwa_wyjściowa: (plik, tryb tła, próg, krawędzie-seedy, erozja px, tylko główna bryła)
#   tryb "const" = jednolite tło (kolor z rogu), "rows" = tło zmienne w pionie (ref. z lewej/prawej krawędzi),
#   "luma" = jasny obiekt na czarnym tle, alfa liczona z jasności (miękka poświata)
SLOPS = {
    # agresywne
    "shadow_watcher":   ("pobierz_2.jfif", "const", 60, "tlrb", 3, False),
    "love_note":        ("love_note.jfif", "const", 40, "tlrb", 1, False),
    "wig_guy":          ("pobierz_6.jfif", "rows", 34, "tlr", 3, True),
    # neutralne
    "sheet_ghost":      ("pobierz_3.jfif", "luma", 0, "", 0, False),
    "cigar_cat":        ("pobierz_5.jfif", "const", 28, "tlr", 1, False),
    # unholy
    "hollow_screamer":  ("the_hollow_screamer.jfif", "const", 45, "tlrb", 2, False),
    "locust":           ("the_locust.jfif", "const", 45, "tlrb", 2, False),
    # nowe (2026-11-26, obrazki autora)
    "cartoon_cat":      ("cartoon_cat.jpg", "const", 45, "tlrb", 1, True),      # agresywny
    "shrek":            ("shrek.jpg", "checker", 0, "tlrb", 1, True),           # neutralny, dla jaj
    "masked_reacher":   ("masked_reacher.jpg", "const", 40, "tlrb", 1, True),   # neutralny: atak po spojrzeniu w oczy (#38)
    "wraith":           ("wraith.png", "alpha", 0, "", 0, False),               # unholy
}


def bg_mask(rgb, mode, thr, edges):
    h, w, _ = rgb.shape
    if mode == "const":
        ref = rgb[2, 2][None, None, :]
    else:
        side = np.concatenate([rgb[:, :4], rgb[:, -4:]], axis=1)
        ref = np.median(side, axis=1)[:, None, :]
    dist = np.sqrt(((rgb - ref) ** 2).sum(-1))
    cand = Image.fromarray(np.where(dist < thr, 255, 0).astype(np.uint8)).copy()
    seeds = []
    if "t" in edges: seeds += [(x, 0) for x in range(w)]
    if "b" in edges: seeds += [(x, h - 1) for x in range(w)]
    if "l" in edges: seeds += [(0, y) for y in range(h)]
    if "r" in edges: seeds += [(w - 1, y) for y in range(h)]
    for s in seeds:
        if cand.getpixel(s) == 255:
            ImageDraw.floodfill(cand, s, 128)
    return np.asarray(cand) == 128


def main_component(fg):
    """Zostawia tylko największą spójną bryłę (usuwa okruchy tła)."""
    m = Image.fromarray(np.where(fg, 255, 0).astype(np.uint8)).copy()
    best, best_n = None, 0
    ys, xs = np.nonzero(fg)
    for i in range(0, len(ys), max(1, len(ys) // 400)):
        x, y = int(xs[i]), int(ys[i])
        if m.getpixel((x, y)) != 255: continue
        before = np.asarray(m).copy()
        ImageDraw.floodfill(m, (x, y), 100)
        n = int(((np.asarray(m) == 100) & (before == 255)).sum())
        a = np.asarray(m).copy(); a[a == 100] = 50; m = Image.fromarray(a).copy()
        if n > best_n: best_n, best = n, (x, y)
    m2 = Image.fromarray(np.where(fg, 255, 0).astype(np.uint8)).copy()
    ImageDraw.floodfill(m2, best, 100)
    return np.asarray(m2) == 100


def checker_mask(rgb, edges):
    """Tło w postaci wtopionej szachownicy „przezroczystości”: jasne i prawie bez nasycenia, spójne z krawędzią."""
    mx, mn = rgb.max(-1), rgb.min(-1)
    cand = (mn > 185) & ((mx - mn) < 14)
    m = Image.fromarray(np.where(cand, 255, 0).astype(np.uint8)).copy()
    h, w = cand.shape
    for x in range(0, w, 2):
        for pt in ((x, 0), (x, h - 1)):
            if m.getpixel(pt) == 255: ImageDraw.floodfill(m, pt, 128)
    for y in range(0, h, 2):
        for pt in ((0, y), (w - 1, y)):
            if m.getpixel(pt) == 255: ImageDraw.floodfill(m, pt, 128)
    return np.asarray(m) == 128


def process(name, src, mode, thr, edges, erode, main_only):
    src_img = Image.open(RAW / src)
    img = src_img.convert("RGB")
    rgb = np.asarray(img).astype(int)
    if mode == "alpha":   # obrazek ma już prawdziwą przezroczystość
        alpha = src_img.convert("RGBA").getchannel("A")
    elif mode == "checker":
        fg = main_component(~checker_mask(rgb, edges)) if main_only else ~checker_mask(rgb, edges)
        alpha = Image.fromarray(np.where(fg, 255, 0).astype(np.uint8)).filter(ImageFilter.MinFilter(2 * erode + 1)).filter(ImageFilter.GaussianBlur(0.8))
    elif mode == "luma":
        luma = rgb.mean(-1)
        a = np.clip((luma - 18) * 2.2, 0, 255)
        # un-premultiply: obiekt był na czarnym tle, więc kolor = obserwowany / alfa
        rgb = np.clip(rgb * 255.0 / np.maximum(a, 1)[..., None], 0, 255)
        img = Image.fromarray(rgb.astype(np.uint8))
        alpha = Image.fromarray(a.astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))
    else:
        fg = ~bg_mask(rgb, mode, thr, edges)
        if main_only:
            fg = main_component(fg)
        alpha = Image.fromarray(np.where(fg, 255, 0).astype(np.uint8))
        if erode:
            alpha = alpha.filter(ImageFilter.MinFilter(2 * erode + 1))  # zjada halo tła
        alpha = alpha.filter(ImageFilter.GaussianBlur(0.8))             # miękka krawędź
    rgba = img.copy(); rgba.putalpha(alpha)
    rgba = rgba.crop(rgba.getbbox())
    s = CANVAS / max(rgba.size)
    rgba = rgba.resize((max(1, round(rgba.width * s)), max(1, round(rgba.height * s))), Image.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.paste(rgba, ((CANVAS - rgba.width) // 2, CANVAS - rgba.height))
    # wyzeruj RGB w pełni przezroczystych pikselach (brak białych obwódek przy filtrowaniu)
    arr = np.asarray(canvas).copy(); arr[arr[..., 3] == 0, :3] = 0
    canvas = Image.fromarray(arr)
    canvas.save(OUT / f"{name}.png", optimize=True)
    # halucynacja: czarna, półprzezroczysta sylwetka
    h = np.zeros_like(arr); h[..., 3] = (arr[..., 3] * HALLU_ALPHA).astype(np.uint8)
    Image.fromarray(h).save(OUT / f"{name}_hallucination.png", optimize=True)
    return canvas, Image.fromarray(h)


def main():
    OUT.mkdir(parents=True, exist_ok=True); PREVIEW.mkdir(parents=True, exist_ok=True)
    tiles = []
    for name, args in SLOPS.items():
        tiles.append(process(name, *args))
    # podgląd: na szachownicy i na ciemnym tle (noc w lesie)
    t = 256; sheet = Image.new("RGBA", (t * len(tiles), t * 3))
    for i, (n, hl) in enumerate(tiles):
        for row, bgc in enumerate([None, (24, 32, 24, 255), (120, 140, 110, 255)]):
            if bgc is None:
                cell = Image.new("RGBA", (t, t), (200, 200, 200, 255))
                d = ImageDraw.Draw(cell)
                for y in range(0, t, 16):
                    for x in range(0, t, 16):
                        if (x + y) // 16 % 2: d.rectangle([x, y, x + 15, y + 15], fill=(150, 150, 150, 255))
            else:
                cell = Image.new("RGBA", (t, t), bgc)
            src = hl if row == 2 else n
            cell.alpha_composite(src.resize((t, t), Image.LANCZOS))
            sheet.paste(cell, (i * t, row * t))
    sheet.save(PREVIEW / "slops_preview.png")


if __name__ == "__main__":
    main()
