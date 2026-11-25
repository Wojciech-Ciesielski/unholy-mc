"""HUD (mózgi Sanity, krople nawodnienia), ikona efektu Wellbeing, woda święcona (fluid), kartki na drzewach, ikona moda."""
import json, math, random
from pathlib import Path
from PIL import Image
from sprites import hexc, render, stack, preview

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc"
TEX = A / "textures"
PREV = ROOT / "assets_src/preview"
OUT = []

def save(img, rel, mcmeta=None):
    p = A / rel; p.parent.mkdir(parents=True, exist_ok=True); img.save(p, optimize=True)
    if mcmeta: (p.parent / (p.name + ".mcmeta")).write_text(json.dumps(mcmeta, indent=2) + "\n")
    OUT.append((rel, img))

def half(img, keep_left):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if (x >= 5) if keep_left else (x < 4): out.putpixel((x, y), (0, 0, 0, 0))
    return out

O = hexc("140e10")
# ------------------------------------------------------------------ Sanity (mózg) 9x9
BRAIN = """
..ooooo..
.oHPpPPo.
oHPpPPpPo
oPpPPpPpo
oPPpPPPpo
opPPpPpPo
.oPPPPppo
..oooopo.
.....oo.."""
BRAIN_EMPTY = BRAIN.replace("H", "x").replace("P", "x").replace("p", "y")
br_full = render(BRAIN, {"o": O, "H": hexc("ffd6df"), "P": hexc("e48aa0"), "p": hexc("a9506a")})
br_cont = render(BRAIN_EMPTY, {"o": O, "x": hexc("3a2a30"), "y": hexc("2a1e23")})
br_blink = render(BRAIN_EMPTY, {"o": hexc("ffffff"), "x": hexc("3a2a30"), "y": hexc("2a1e23")})
# wariant "niski Sanity" – zgniły, szarozielony
br_rot = render(BRAIN, {"o": O, "H": hexc("b9c2a0"), "P": hexc("7f8a62"), "p": hexc("4b5238")}, grime=0.3, seed=11, protect="o")
save(br_cont, "textures/gui/sprites/hud/sanity/container.png")
save(br_blink, "textures/gui/sprites/hud/sanity/container_blinking.png")
save(br_full, "textures/gui/sprites/hud/sanity/full.png")
save(half(br_full, True), "textures/gui/sprites/hud/sanity/half.png")
save(br_rot, "textures/gui/sprites/hud/sanity/full_low.png")
save(half(br_rot, True), "textures/gui/sprites/hud/sanity/half_low.png")

# ------------------------------------------------------------------ Nawodnienie (kropla) 9x9
DROP = """
....o....
...oBo...
...oBo...
..oBwBo..
.oBwBBBo.
.oBBBBbo.
.oBBBbbo.
..obbbo..
...ooo..."""
DROP_EMPTY = DROP.replace("B", "x").replace("w", "x").replace("b", "y")
dr_full = render(DROP, {"o": O, "B": hexc("3f8fe0"), "w": hexc("d6f0ff"), "b": hexc("2458a8")})
dr_holy = render(DROP, {"o": O, "B": hexc("8fd3ee"), "w": hexc("ffffff"), "b": hexc("5a9fc4")})
dr_dirty = render(DROP, {"o": O, "B": hexc("6f7f5a"), "w": hexc("b5c09a"), "b": hexc("48553a")}, grime=0.3, seed=12, protect="o")
save(render(DROP_EMPTY, {"o": O, "x": hexc("2a3038"), "y": hexc("1d2228")}), "textures/gui/sprites/hud/hydration/container.png")
save(dr_full, "textures/gui/sprites/hud/hydration/full.png")
save(half(dr_full, False), "textures/gui/sprites/hud/hydration/half.png")
save(dr_dirty, "textures/gui/sprites/hud/hydration/full_dehydrated.png")
save(half(dr_dirty, False), "textures/gui/sprites/hud/hydration/half_dehydrated.png")

# ------------------------------------------------------------------ efekt Wellbeing 18x18 (czterolistna koniczyna + iskra)
WELL = """
..................
..........w.......
.....ooo.wyw......
....oGGgo.w.......
....oGgggo.ooo....
....oggggoogGGo...
.....oggggogggo...
...ooooggoggggo...
..oGGgooggogggo...
.oGggggooooooo....
.oggggggoGGgoo....
..ogggggoggggo....
...oooooggggggo...
.......oogggggo...
......oso.oggo....
.....oso...oo.....
.....oo...........
.................."""
save(render(WELL, {"o": hexc("0f2a14"), "G": hexc("8ee07a"), "g": hexc("3fa046"), "s": hexc("5a3d26"),
                   "w": hexc("fff4b0"), "y": hexc("ffd23f")}), "textures/mob_effect/wellbeing.png")

# ------------------------------------------------------------------ woda święcona (fluid) – still 16x16, flow 32x32, 32 klatki
def water_frames(size, n, flow):
    rnd = random.Random(99)
    sparks = [(rnd.randrange(size), rnd.randrange(size), rnd.randrange(n)) for _ in range(size // 4)]
    frames = []
    for f in range(n):
        t = f / n * 2 * math.pi
        img = Image.new("RGBA", (size, size))
        for y in range(size):
            for x in range(size):
                yy = (y - f * size / n) if flow else y      # flow: przewija się w dół (pętla)
                v = (math.sin((x / size) * 2 * math.pi * 2 + t) + math.sin((yy / size) * 2 * math.pi * 3 - t)
                     + math.sin(((x + yy) / size) * 2 * math.pi * 2 + 2 * t)) / 3
                r, g, b = 150 + 25 * v, 214 + 18 * v, 240 + 12 * v
                img.putpixel((x, y), (int(r), int(g), int(min(255, b)), 200))
        for sx, sy, sf in sparks:                          # migoczące iskierki (poświata)
            k = (f - sf) % n
            if k < 3:
                yy = int((sy + f * size / n) % size) if flow else sy
                img.putpixel((sx, yy), (255, 255, 255, 235 - k * 40))
        frames.append(img)
    return stack(frames)
save(water_frames(16, 32, False), "textures/block/holy_water_still.png", {"animation": {"frametime": 2}})
save(water_frames(32, 32, True), "textures/block/holy_water_flow.png", {"animation": {"frametime": 2}})

# ------------------------------------------------------------------ kartki na drzewach (block, cutout)
PAPER = {"o": hexc("3b3226"), "P": hexc("d9cfb4"), "p": hexc("bfb393"), "q": hexc("a39874"),
         "i": hexc("2b2622"), "R": hexc("7d0d0d"), "r": hexc("a3171a"), "n": hexc("6e6a66")}
NOTE_HELP = """
................
.......n........
..oooooooooooo..
..oPPPpPPPPPPo..
..oPiPiPiiiPPo..
..oPiPiPiPPPpo..
..oPiiiPiiPPPo..
..oPiPiPiPPpPo..
..oPiPiPiiiPPo..
..opPPPPPPPPPo..
..oPiPPPiiiPPo..
..oPiPPPiPiPpo..
..oPiPPPiiiPPo..
..oPiPPqiPPPPo..
..oPiiiPiPPqpo..
..oooooooooooo.."""
NOTE_HEISHERE = """
................
.......n........
..oooooooooooo..
..oPiPiPiiPPPo..
..oPPPPPPPPPpo..
..oPiiPiPiiPPo..
..oPPPPPPPPPPo..
..oPPPPPiPPPPo..
..oPPPPiiiPPpo..
..oPPPPPiPPPPo..
..oPPPPiPiPPPo..
..oPpPPiPiPPPo..
..oPPPiPPPiPPo..
..oPPPiPPPiPPo..
..oPqPPPPPPqpo..
..oooooooooooo.."""
NOTE_BLOOD = """
................
.......n........
..oooooooooooo..
..oPPRrPPPPPPo..
..oPRRRrPPpPPo..
..oPPRrRPPPPPo..
..oPPPRPPRPPPo..
..oPiPiPRrRPpo..
..oPPPPPRRrPPo..
..oPiiPPPRPPPo..
..oPPPPPPRPPPo..
..oPpPRPPPPPPo..
..oPPRrRPiiPPo..
..oPPPRPPPPPPo..
..oPqPPPPPPqpo..
..oooooooooooo.."""
NOTE_SYMBOL = """
................
.......n........
..oooooooooooo..
..oPPPPPPPPPPo..
..oPPPiiiiPPpo..
..oPPiPPPPiPPo..
..oPiPiPPiPiPo..
..oPiPPiiPPiPo..
..oPiPPiiPPiPo..
..oPiPiPPiPiPo..
..oPPiPPPPiPPo..
..oPPPiiiiPPPo..
..oPpPPPPPPPPo..
..oPPPPqPPPPpo..
..oPqPPPPPPPPo..
..oooooooooooo.."""
for i, (n, g) in enumerate([("help", NOTE_HELP), ("he_is_here", NOTE_HEISHERE),
                            ("blood", NOTE_BLOOD), ("symbol", NOTE_SYMBOL)]):
    save(render(g, PAPER, grime=0.12, seed=20 + i, protect="oiRrn"), f"textures/block/tree_note_{n}.png")

# ------------------------------------------------------------------ ikona moda 128x128
gem = Image.open(TEX / "item/nightmare_gem.png").resize((96, 96), Image.NEAREST)
icon = Image.new("RGBA", (128, 128), (10, 6, 12, 255))
for y in range(128):
    for x in range(128):
        d = math.hypot(x - 64, y - 64) / 64
        k = max(0, 1 - d)
        icon.putpixel((x, y), (int(10 + 60 * k * k), int(4 + 4 * k), int(12 + 20 * k * k), 255))
icon.alpha_composite(gem, (16, 16))
save(icon, "icon.png")

if __name__ == "__main__":
    preview([(r, i) for r, i in OUT if i.width <= 32], PREV / "gui_blocks_preview.png", cols=8)
    print("\n".join(r for r, _ in OUT))
