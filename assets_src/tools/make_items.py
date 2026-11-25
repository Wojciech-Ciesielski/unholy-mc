"""Generuje tekstury itemów 16x16 (styl: mroczny / brudny) i animacje .mcmeta.

Użycie: python3 assets_src/tools/make_items.py
"""
import json, math
from pathlib import Path
from PIL import Image
from sprites import hexc, render, stack, preview

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/unholymc/textures"
ITEM = TEX / "item"
PREV = ROOT / "assets_src/preview"
O = hexc("1a1416")
OUT = []

def save(img, rel, mcmeta=None):
    p = TEX / rel; p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p, optimize=True)
    if mcmeta:
        (p.parent / (p.name + ".mcmeta")).write_text(json.dumps(mcmeta, indent=2) + "\n")
    OUT.append((rel, img))

# woda święcona: używa vanilla minecraft:item/potion + potion_overlay z tintem (patrz make_models.py)

# ------------------------------------------------------------------ krucyfiks
CRUCIFIX = """
......abbb......
......acdb......
......acdb......
......acdb......
.aaaaaacdaaaaab.
.acccccTtccccdb.
.adddddtudddddb.
.abbbbbcdbbbbbb.
......acdb......
......acdb......
......acdb......
......acdb......
......acdb......
......acdb......
......abbb......
................"""
# drewno w palecie vanilla patyka + łza ghasta w środku
save(render(CRUCIFIX, {"a": hexc("493615"), "c": hexc("896727"), "d": hexc("684e1e"), "b": hexc("281e0b"),
                       "T": hexc("f4fffd"), "t": hexc("b4e4de"), "u": hexc("6fb3ab")},
            grime=0.08, seed=3, protect="abTtu"), "item/crucifix.png")

# ------------------------------------------------------------------ diagonalne itemy (oś: lewy-dół -> prawy-góra)
def diag(fn, size=16):
    """fn(a, c) -> znak; a = wzdłuż osi (x - y), c = w poprzek (x + y - 15)."""
    rows = []
    for y in range(size):
        rows.append("".join(fn(x - y, x + y - (size - 1)) or "." for x in range(size)))
    # obrys
    g = [list(r) for r in rows]
    for y in range(size):
        for x in range(size):
            if g[y][x] != ".": continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < size and 0 <= yy < size and rows[yy][xx] not in ".o":
                    g[y][x] = "o"; break
    return "\n".join("".join(r) for r in g)

def flashlight_fn(a, c):
    if -13 <= a <= -1 and abs(c) <= 2:                 # trzonek (3 px)
        if a in (-5, -6) and c <= -1: return "P"      # przycisk
        if a in (-11, -9) : return "r"                # rowki uchwytu
        return "B" if c < 0 else ("b" if c == 0 else "d")
    if 0 <= a <= 8 and abs(c) <= (3 if a < 4 else 4):  # głowica (rozszerzona)
        if a >= 7: return "L" if c > -3 else "l"        # soczewka (rdzeń)
        if a in (3, 4): return "h" if c < 2 else "d"    # pierścień
        return "H" if c < 0 else ("h" if c <= 1 else "d")
    return None

FLASH = diag(flashlight_fn)
FL_PAL = {"o": O, "B": hexc("6b6a6e"), "b": hexc("4c4b50"), "d": hexc("2e2d32"), "r": hexc("26252a"),
          "P": hexc("7d1f1f"), "H": hexc("8a8580"), "h": hexc("5f5a56")}
CORES = {  # nazwa: (soczewka jasna, soczewka ciemna)
    "none":         ("cfd3c8", "8e9389"),
    "lapis":        ("3d64d8", "22398a"),
    "redstone":     ("e0261b", "8f140e"),
    "emerald":      ("2ed46a", "137a3a"),
    "diamond":      ("7fe8e0", "3aa8a1"),
    "netherite":    ("6a5d63", "3b3237"),
    "nether_star":  ("ffffff", "d8e4f0"),
    "nightmare_gem": ("4a1a5c", "120818"),
}
for core, (hi, lo) in CORES.items():
    pal = dict(FL_PAL, L=hexc(hi), l=hexc(lo))
    save(render(FLASH, pal, grime=0.14, seed=4, protect="oLlP"), f"item/flashlight_{core}.png")
# nakładka "włączona": poświata przed soczewką, osobna warstwa (layer1)
def glow_layer(color):
    rows = FLASH.split("\n")
    lens = [(x, y) for y in range(16) for x in range(16) if rows[y][x] in "Ll"]
    amax = max(x - y for x, y in lens)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if rows[y][x] != "." or x - y < amax: continue
            d = min(max(abs(x - lx), abs(y - ly)) for lx, ly in lens)
            if d <= 3:
                img.putpixel((x, y), hexc(color, (190, 120, 60)[d - 1] if d >= 1 else 190))
    return img
save(glow_layer("fff6c8"), "item/flashlight_beam.png")
save(glow_layer("12001a"), "item/flashlight_beam_dark.png")

# ------------------------------------------------------------------ Unholy Sword (sylwetka w stylu vanilla)
SWORD = """
.............ooo
............oHHo
...........oHvDo
..........oHvDo.
.........oHvDDo.
........oHvDoo..
.o.....oMvDo....
oGo...oMvDo.....
.oGo.oMvDo......
..oGgGvDo.......
...ogGgo........
...owoGgo.......
..oWwo.oGo......
.oWwo...oGo.....
opwo.....o......
ooo............."""
rows = SWORD.strip("\n").split("\n")
blade = [(x, y) for y in range(16) for x in range(16) if rows[y][x] in "HMvD"]
CHG = {"o": hexc("1c0508"), "H": hexc("ff7a66"), "M": hexc("d02a2e"), "D": hexc("7a0c18"),
       "G": hexc("e2d6b8"), "g": hexc("9c8f70"), "w": hexc("2a1414"), "W": hexc("4e2424"), "p": hexc("ff2a3a")}
# cząsteczki: żarzące się drobinki unoszące się znad ostrza (pozycje startowe w pustych pikselach przy ostrzu)
starts = [(9, 4), (12, 7), (6, 6), (13, 3), (10, 8), (7, 3), (11, 9), (8, 2)]
EMBER = [hexc("ffd27a"), hexc("ff4a3a"), hexc("b0102a"), hexc("4a0a2a", 200)]
N = 12
frames = []
for i in range(N):
    t = (1 - math.cos(i / N * 2 * math.pi)) / 2
    vein = tuple(int(lo + (hi - lo) * t) for lo, hi in zip((150, 10, 20), (255, 200, 120))) + (255,)
    img = render(SWORD, dict(CHG, v=vein), grime=0.06, seed=5, protect="ovpHGg")
    for k, (sx, sy) in enumerate(starts):
        age = (i + k * 3) % N
        if age >= 4: continue                       # każda drobinka żyje 4 klatki, potem przerwa
        x, y = sx - (age // 2) * (k % 2), sy - age  # unosi się (lekko w lewo co druga)
        if 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3] == 0:
            img.putpixel((x, y), EMBER[age])
    frames.append(img)
save(stack(frames), "item/unholy_sword.png", {"animation": {"frametime": 2}})
# rozładowany: popielate, spękane ostrze, zszarzały jelec, martwy kamień, brak cząsteczek
DIS = {"o": hexc("141212"), "H": hexc("8a8582"), "M": hexc("5c5856"), "D": hexc("343130"), "v": hexc("1e1b1b"),
       "G": hexc("8f8676"), "g": hexc("5e5749"), "w": hexc("221c1c"), "W": hexc("3a3030"), "p": hexc("4a2c2c")}
dis = render(SWORD, DIS, grime=0.25, seed=6, protect="o")
for x, y in [(11, 3), (9, 5), (8, 7)]:                # pęknięcia
    dis.putpixel((x, y), hexc("0e0c0c"))
save(dis, "item/unholy_sword_discharged.png")

# ------------------------------------------------------------------ termos (klasyczna zielona piersiówka z kubkiem i rączką)
THERMOS = """
......oooo......
.....oCCcco.....
.....occcco.....
.....oSSsso.....
....oGGGggdo....
....oHGGggdo....
....oHGGggdo....
....oHGFGgdo....
....oHFfFgdo....
....oHFFFgdo....
....oHGGggdo....
....oHGGggdo....
....oGGGggdo....
....oSSSssso....
....oSSsssso....
.....oooooo....."""
TH_PAL = {"o": hexc("16200f"), "C": hexc("d6d6d2"), "c": hexc("8f8f8a"), "H": hexc("86b46a"), "G": hexc("5d8a44"), "g": hexc("426a30"),
          "d": hexc("2a4520"), "S": hexc("bdbdb8"), "s": hexc("7a7a75")}
FILLS = {"water": ("3f76e4", "7fa8f5"), "holy_water": ("8fd3ee", "eefaff"),
         "honey": ("e8a12a", "fbe08a"), "milk": ("e9e6dc", "ffffff")}
save(render(THERMOS.replace("F", "G").replace("f", "G"), TH_PAL, grime=0.12, seed=7, protect="o"), "item/thermos_empty.png")
for name, (body, top) in FILLS.items():
    save(render(THERMOS, dict(TH_PAL, F=hexc(body), f=hexc(top)), grime=0.12, seed=7, protect="oFf"),
         f"item/thermos_{name}.png")

# ------------------------------------------------------------------ koszmarny klejnot
GEM = """
................
................
......oooo......
.....oHLLmo.....
....oHLmmMMO....
...oHLLmmMMdO...
...oLmmMMrMdO...
...oLmmMrRrdO...
...oLmMMrRrdO...
...oLmMMMrMdO...
...omMMMMdMdO...
....omMdddMO....
.....oMddMO.....
......OOOO......
................
................"""
# fasetowany klejnot w kształcie emeralda: fiolet nocy z czerwonym rdzeniem
save(render(GEM, {"o": hexc("2a0a36"), "O": hexc("12031a"), "H": hexc("f0c8ff"), "L": hexc("b060d0"),
                  "m": hexc("7a2a98"), "M": hexc("561a70"), "d": hexc("34104a"),
                  "r": hexc("c01838"), "R": hexc("ff4a5a")}), "item/nightmare_gem.png")

if __name__ == "__main__":
    preview(OUT, PREV / "items_preview.png")
    print("\n".join(r for r, _ in OUT))
