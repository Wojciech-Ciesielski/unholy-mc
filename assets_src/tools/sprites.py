"""Wspólne narzędzia pixel-art: ASCII grid -> PNG, brud, podgląd."""
import random
from PIL import Image

def hexc(s, a=255):
    s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)

def render(grid, pal, grime=0.0, seed=0, protect="o"):
    rows = [r for r in grid.strip("\n").split("\n")]
    h, w = len(rows), max(len(r) for r in rows)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rnd = random.Random(seed)
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in ". ": continue
            c = pal[ch]
            if grime and ch not in protect and rnd.random() < grime:
                f = rnd.choice([0.78, 0.85, 0.9, 1.08])
                c = tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)
            img.putpixel((x, y), c)
    return img

def stack(frames):
    w, h = frames[0].size
    out = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames): out.paste(f, (0, i * h))
    return out

def preview(items, path, scale=8, cols=8, bg=(54, 54, 60, 255)):
    """items: list of (name, img). Zapisuje arkusz podglądu w powiększeniu."""
    cell = 16 * scale + 8
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), bg)
    for i, (_, im) in enumerate(items):
        fr = im.convert("RGBA").crop((0, 0, im.width, min(im.height, im.width)))
        s = (16 * scale) // fr.width
        fr = fr.resize((fr.width * s, fr.height * s), Image.NEAREST)
        sheet.alpha_composite(fr, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    sheet.save(path)
