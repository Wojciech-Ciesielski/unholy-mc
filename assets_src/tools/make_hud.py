"""Nowy HUD (DECISIONS #34): dynamiczne ikony statystyk 16x16 (wyrównane) i cyfry. Bez panelu i bez pancerza (decyzja autora).

Wynik: textures/font/hud_stats/*.png + font/hud_stats.json (znaki w docs/ASSETS.md §4).
"""
import json
from pathlib import Path
from PIL import Image
from sprites import hexc, preview

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc"
TEX = A / "textures/font/hud_stats"
PREV = ROOT / "assets_src/preview"
OUT = []

HEART = """
................
..oooo....oooo..
.oRRRRo..oRRRRo.
oRWWRRRooRRRRRro
oRWRRRRRRRRRRRro
oRRRRRRRRRRRRRro
oRRRRRRRRRRRRrro
.oRRRRRRRRRRrro.
..oRRRRRRRRrro..
...oRRRRRRrro...
....oRRRRrro....
.....oRRrro.....
......oRro......
.......oo.......
................
................"""
MEAT = """
................
..........ooo...
........ooMMMoo.
.......oMHHMMMmo
......oMHMMMMMmo
......oMMMMMMMmo
.....oMMMMMMMmmo
.....oMMMMMMmmo.
....obMMMMmmmo..
...oBboommmoo...
..oBbo..ooo.....
.oBbo...........
oWBo............
oWWo............
.oo.............
................"""
DROP = """
................
.......oo.......
......oBBo......
......oBBo......
.....oBWBBo.....
.....oBWBBo.....
....oBWBBBBo....
...oBBWBBBBbo...
...oBWBBBBBbo...
..oBBBBBBBBBbo..
..oBBBBBBBBBbo..
..oBBBBBBBBbbo..
...oBBBBBBbbo...
....obbbbbbo....
.....oooooo.....
................"""
BRAIN = """
................
................
....oooooo......
..ooPPpPPPoo....
.oPHPpPPpPPPo...
oPHPPPpPPPpPPo..
oPPpPPPPpPPPPPo.
oPpPPpPPPPpPpPo.
oPPPpPPpPPPPPpPo
opPPPPPpPPpPPPPo
.oPPpPPPPPPpPpo.
..ooPPPpPPPPoo..
....ooooPPpo....
.......opPo.....
........oo......
................"""


def grid(s):
    return [list(r) for r in s.strip("\n").split("\n")]


def outline(g, ch="o"):
    """Usuwa stary obrys i rysuje nowy wokół wypełnienia."""
    h, w = len(g), len(g[0])
    g = [["." if c == "o" else c for c in r] for r in g]
    out = [r[:] for r in g]
    for y in range(h):
        for x in range(w):
            if g[y][x] != ".": continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < w and 0 <= yy < h and g[yy][xx] != ".":
                    out[y][x] = ch; break
    return out


def paint(g, pal, name):
    img = Image.new("RGBA", (16, 16))
    for y, r in enumerate(g):
        for x, c in enumerate(r):
            if c != ".": img.putpixel((x, y), pal[c])
    TEX.mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{name}.png")
    OUT.append((name, img))
    return img


def put(g, pts, c, only=None):
    for x, y in pts:
        if 0 <= y < len(g) and 0 <= x < len(g[0]) and (only is None or g[y][x] in only): g[y][x] = c
    return g


def shift(g, dx):
    w = len(g[0])
    return [[r[x - dx] if 0 <= x - dx < w else "." for x in range(w)] for r in g]


O = hexc("1a0f10")
# ------------------------------------------------------------------ zdrowie
HP = {"o": O, "W": hexc("ffd0d0"), "R": hexc("e0202a"), "r": hexc("8e0e18"), "x": hexc("3a0508")}
def heart_stage(stage):
    g = grid(HEART)
    if stage >= 1: put(g, [(9, 3), (8, 4), (9, 5), (8, 6)], "x", "RWr")
    if stage >= 2:
        put(g, [(9, 7), (8, 8), (9, 9), (8, 10)], "x", "Rr")
        put(g, [(x, y) for y in range(2, 5) for x in range(10, 15) if (x - 12) ** 2 + (y - 3) ** 2 <= 3], ".")
        g = outline(g)
    if stage >= 3:  # pęknięte na pół
        left = [[c if x < 8 else "." for x, c in enumerate(r)] for r in g]
        right = [[c if x >= 8 else "." for x, c in enumerate(r)] for r in g]
        L, Rr = shift(left, -1), shift(right, 1)
        g = [[a if a != "." else b for a, b in zip(ra, rb)] for ra, rb in zip(L, Rr)]
        g = outline([["." if c == "x" else c for c in r] for r in g])
    return g
pal_stage = [HP, dict(HP, R=hexc("cc1c26")), dict(HP, R=hexc("a8141e"), W=hexc("e08a8a")), dict(HP, R=hexc("7a0c14"), r=hexc("4a060c"), W=hexc("a04040"))]
for i, n in enumerate(["full", "medium", "low", "critical"]):
    paint(heart_stage(i), pal_stage[i], f"health_{n}")
paint(heart_stage(3), dict(HP, o=hexc("ff3030"), R=hexc("c01020")), "health_critical_blink")
paint(heart_stage(0), dict(HP, R=hexc("4a9a20"), r=hexc("2a5a10"), W=hexc("b8e890")), "health_poison")
paint(heart_stage(0), dict(HP, R=hexc("2a2a2a"), r=hexc("141414"), W=hexc("6a6a6a")), "health_wither")
paint(heart_stage(0), dict(HP, R=hexc("8ac8f0"), r=hexc("4a88b8"), W=hexc("ffffff")), "health_frozen")
paint(heart_stage(0), dict(HP, R=hexc("f0c020"), r=hexc("a07810"), W=hexc("fff4b0")), "health_absorption")

# ------------------------------------------------------------------ głód (udko)
MP = {"o": hexc("1f1008"), "M": hexc("b5653a"), "H": hexc("e8a070"), "m": hexc("7a3a1c"), "B": hexc("e8e0cc"),
      "b": hexc("b0a68c"), "W": hexc("ffffff"), "x": hexc("5a5040")}
def meat_stage(stage):
    g = grid(MEAT)
    r = [0, 3.2, 6.0, 99][stage]
    put(g, [(x, y) for y in range(16) for x in range(16) if (x - 14) ** 2 + (y - 2) ** 2 <= r * r], ".", "MHm")
    if stage == 3:
        put(g, [(x, y) for y in range(16) for x in range(16)], ".", "MHm")
        put(g, [(4, 8), (5, 8), (4, 9)], "B", ".")              # koniec kości
        put(g, [(2, 11), (3, 10)], "x")                         # pęknięcie
    return outline(g)
for i, n in enumerate(["full", "medium", "low", "critical"]):
    paint(meat_stage(i), MP, f"hunger_{n}")
paint(meat_stage(3), dict(MP, o=hexc("ff3030")), "hunger_critical_blink")
paint(meat_stage(0), dict(MP, M=hexc("7a9a3a"), H=hexc("b0d070"), m=hexc("4a6a1c")), "hunger_effect")

# ------------------------------------------------------------------ nawodnienie (kropla)
DP = {"o": hexc("0c1420"), "B": hexc("3f8fe0"), "W": hexc("d6f0ff"), "b": hexc("2458a8"), "e": hexc("1a2a3a", 150),
      "x": hexc("8a7a5a")}
def drop_stage(stage):
    g = grid(DROP)
    level = [0, 7, 10, 99][stage]       # wiersze powyżej poziomu są puste
    for y in range(16):
        for x in range(16):
            if y < level and g[y][x] in "BWb": g[y][x] = "e"
    if stage >= 1:  # powierzchnia wody
        for x in range(16):
            if level < 16 and g[level][x] in "Bb": g[level][x] = "W"
    if stage == 3: put(g, [(7, 5), (8, 7), (7, 9), (8, 11)], "x")    # sucha, spękana
    return g
for i, n in enumerate(["full", "medium", "low", "critical"]):
    paint(drop_stage(i), DP, f"hydration_{n}")
paint(drop_stage(3), dict(DP, o=hexc("ff3030")), "hydration_critical_blink")

# ------------------------------------------------------------------ sanity (mózg)
BP = {"o": hexc("1a0c12"), "P": hexc("e48aa0"), "p": hexc("a9506a"), "H": hexc("ffd6df"), "x": hexc("2a1a14"),
      "k": hexc("0a0608"), "E": hexc("f4e8d0"), "e": hexc("c01020")}
def gyri(g):
    """Faliste zwoje zamiast przypadkowych kropek + bruzda boczna."""
    import math
    for y, r in enumerate(g):
        for x, c in enumerate(r):
            if c in "Pp":
                v = (x + 2.2 * math.sin(y * 0.95 + x * 0.3)) % 4
                r[x] = "p" if v < 1.0 else "P"
    for x, y in [(3, 9), (4, 9), (5, 9), (6, 8), (7, 8), (8, 8), (9, 9), (10, 9)]:
        if g[y][x] in "Pp": g[y][x] = "p"
    return g


def brain_stage(stage):
    g = gyri(grid(BRAIN))
    if stage >= 2: put(g, [(4, 7), (9, 5), (11, 9), (6, 10)], "x", "Pp")
    if stage >= 3:
        put(g, [(7, 7), (8, 7), (7, 8), (8, 8)], "E")             # oko w środku
        put(g, [(8, 8)], "e")
        put(g, [(3, 11), (3, 12), (12, 11), (12, 12), (12, 13), (5, 12)], "k")  # czarne krople
        g = outline(g)
    return g
brain_pals = [BP, dict(BP, P=hexc("c99aa4"), p=hexc("8a6470"), H=hexc("eed8dc")),
              dict(BP, P=hexc("8f9a72"), p=hexc("5a6444"), H=hexc("c4ccaa")),
              dict(BP, P=hexc("5a5a40"), p=hexc("34361f"), H=hexc("7e7e60"))]
for i, n in enumerate(["full", "medium", "low", "critical"]):
    paint(brain_stage(i), brain_pals[i], f"sanity_{n}")
paint(shift(brain_stage(3), 1), dict(brain_pals[3], o=hexc("ff3030")), "sanity_critical_jitter")
wb = brain_stage(0)
put(wb, [(14, 2), (1, 3), (15, 11), (0, 12)], "S", ".")
paint(wb, dict(BP, P=hexc("ffd24a"), p=hexc("c08a18"), H=hexc("fff6c0"), S=hexc("fff4b0")), "sanity_wellbeing")

# ------------------------------------------------------------------ cyfry 5x7 z obrysem (komórka 7x9)
DIG = {
    "0": [" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "],
    "1": ["  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "],
    "2": [" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"],
    "3": ["#####", "   # ", "  #  ", "   # ", "    #", "#   #", " ### "],
    "4": ["   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "],
    "5": ["#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "],
    "6": ["  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### "],
    "7": ["#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "],
    "8": [" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "],
    "9": [" ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  "],
    "+": ["     ", "  #  ", "  #  ", "#####", "  #  ", "  #  ", "     "],
    "/": ["    #", "    #", "   # ", "  #  ", " #   ", "#    ", "#    "],
}
strip = Image.new("RGBA", (7 * len(DIG), 9))
for k, (c, rows) in enumerate(DIG.items()):
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == "#":
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        px, py = k * 7 + x + 1 + dx, y + 1 + dy
                        if strip.getpixel((px, py))[3] == 0: strip.putpixel((px, py), (20, 12, 14, 255))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == "#": strip.putpixel((k * 7 + x + 1, y + 1), (255, 255, 255, 255))
strip.save(TEX / "digits.png")

# ------------------------------------------------------------------ wyrównanie: każda statystyka wyśrodkowana na wspólnej ramce
def align(prefix):
    files = sorted(TEX.glob(f"{prefix}_*.png"))
    ims = [Image.open(f).convert("RGBA") for f in files]
    boxes = [im.getbbox() for im in ims]
    x0 = min(b[0] for b in boxes); y0 = min(b[1] for b in boxes); x1 = max(b[2] for b in boxes); y1 = max(b[3] for b in boxes)
    dx = (16 - (x1 - x0)) // 2 - x0; dy = (16 - (y1 - y0)) // 2 - y0
    for f, im in zip(files, ims):
        out = Image.new("RGBA", (16, 16)); out.paste(im, (dx, dy)); out.save(f)
for prefix in ("health", "hunger", "hydration", "sanity"):
    align(prefix)
for old in ["panel", "armor_slot"] + [f"durability_{i}" for i in range(9)]:
    (TEX / f"{old}.png").unlink(missing_ok=True)

# ------------------------------------------------------------------ font
prov = []
def bm(file, c, h=16, asc=12):
    prov.append({"type": "bitmap", "file": f"unholymc:font/hud_stats/{file}.png", "height": h, "ascent": asc, "chars": [chr(c) if isinstance(c, int) else c]})
names = {
    0xE400: ["health_full", "health_medium", "health_low", "health_critical", "health_critical_blink",
             "health_poison", "health_wither", "health_frozen", "health_absorption"],
    0xE410: ["hunger_full", "hunger_medium", "hunger_low", "hunger_critical", "hunger_critical_blink", "hunger_effect"],
    0xE420: ["hydration_full", "hydration_medium", "hydration_low", "hydration_critical", "hydration_critical_blink"],
    0xE430: ["sanity_full", "sanity_medium", "sanity_low", "sanity_critical", "sanity_critical_jitter", "sanity_wellbeing"],
}
for base, lst in names.items():
    for i, n in enumerate(lst): bm(n, base + i)
prov.append({"type": "bitmap", "file": "unholymc:font/hud_stats/digits.png", "height": 9, "ascent": 9,
             "chars": ["".join(chr(0xE480 + i) for i in range(len(DIG)))]})
prov.append({"type": "bitmap", "file": "unholymc:font/hud_stats/digits.png", "height": 9, "ascent": 8, "chars": ["0123456789+/"]})
prov.append({"type": "space", "advances": {chr(0xE4F0): -1, chr(0xE4F1): -2, chr(0xE4F2): -4, chr(0xE4F3): -8,
                                           chr(0xE4F4): -16, chr(0xE4F5): -21, chr(0xE4F6): -45, chr(0xE4F7): 2, chr(0xE4F8): 4}})
(A / "font").mkdir(exist_ok=True)
(A / "font/hud_stats.json").write_text(json.dumps({"providers": prov}, indent=1) + "\n")

# podgląd na tle trawy/nieba
pv = Image.new("RGBA", (16 * 9 * 6, 16 * 4 * 6 + 60), (92, 128, 70, 255))
order = [names[0xE400], names[0xE410], names[0xE420], names[0xE430]]
for r, lst in enumerate(order):
    for c, n in enumerate(lst):
        pv.alpha_composite(Image.open(TEX / f"{n}.png").resize((96, 96), Image.NEAREST), (c * 96, r * 96))
pv.alpha_composite(strip.resize((strip.width * 5, 45), Image.NEAREST), (0, 16 * 4 * 6 + 8))
pv.save(PREV / "hud_stats_preview.png")
