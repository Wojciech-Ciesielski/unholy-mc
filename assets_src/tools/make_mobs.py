"""Moby 3D (DECISIONS #36): 8 slopów jako modele z części (jak Slenderman), tekstury 128×128 z gęstością 2 tekseli/px.

Na wyjściu:
  textures/entity/mob/<mob>.png, <mob>_hallucination.png
  models/entity_part/<mob>_<part>.json (+ _hallucination)
  items/mob/<mob>/<part>.json      (select: custom_model_data.strings[0] == "hallucination" → wersja czarna)
  ../../data/unholymc/mob_models/<mob>.json  – części, punkty obrotu (bloki od stóp), rola w animacji, hitbox
Podgląd: assets_src/preview/mobs_preview.png (rzut z przodu i z boku).

Układ współrzędnych części: jak w modelach encji vanilla (px, y w dół od punktu obrotu).
"""
import json, math
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc"
DATA = ROOT / "src/main/resources/data/unholymc/mob_models"
PREV = ROOT / "assets_src/preview"
TEX = 128          # szerokość tekstury
TEXH = 128         # wysokość (128 albo 256, ustawiane per mob przez pack)
K = 2   # tekseli na piksel modelu
rng = np.random.default_rng(7)


def hexc(s):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


# =================================================================== geometria
class Box:
    def __init__(self, o, s, mat, faces=None, inflate=0):
        self.o, self.s, self.mat, self.faces = o, s, mat, faces or {}
        self.uv = None

    def uv_size(self):
        w, h, d = self.s
        return (2 * (w + d)) * K, (d + h) * K


class Part:
    def __init__(self, name, pivot, boxes, role="static", emissive=False, extra=None):
        self.name, self.pivot, self.boxes, self.role, self.emissive = name, pivot, boxes, role, emissive
        self.extra = extra or {}


def pack(boxes):
    """Proste pakowanie półkowe; 128×128, a gdy się nie mieści – 128×256. Zwraca wysokość tekstury."""
    items = sorted(boxes, key=lambda b: -b.uv_size()[1])
    for H in (128, 256):
        x = y = shelf = 0; ok = True
        for b in items:
            w, h = b.uv_size()
            w, h = math.ceil(w), math.ceil(h)
            if x + w > TEX: x, y, shelf = 0, y + shelf, 0
            if y + h > H: ok = False; break
            b.uv = (x, y); x += w; shelf = max(shelf, h)
        if ok: return H
    raise RuntimeError("tekstura za mała")


def face_rects(b):
    w, h, d = [v * K for v in b.s]
    u, v = b.uv
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "west": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}


# =================================================================== malowanie
def vnoise(h, w, scale=4, seed=None):
    r = np.random.default_rng(seed if seed is not None else rng.integers(1 << 30))
    gh, gw = h // scale + 2, w // scale + 2
    g = r.random((gh, gw))
    yy = np.linspace(0, gh - 2, h); xx = np.linspace(0, gw - 2, w)
    y0 = yy.astype(int); x0 = xx.astype(int); fy = (yy - y0)[:, None]; fx = (xx - x0)[None, :]
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


MATS = {}  # nazwa -> dict(base, dark, noise, grain, vgrad, pattern, alpha)


def mat(name, base, dark=None, noise=0.18, grain=10, vgrad=0.15, pattern=None, alpha=255):
    MATS[name] = dict(base=hexc(base), dark=hexc(dark or base) if dark else hexc(base) * 0.6, noise=noise, grain=grain,
                      vgrad=vgrad, pattern=pattern, alpha=alpha)


SIDE_SHADE = {"up": 1.08, "down": 0.7, "north": 1.0, "south": 0.82, "west": 0.9, "east": 0.9}


def paint_face(img, rect, m, face, seed):
    x, y, w, h = [int(round(v)) for v in rect]
    if w <= 0 or h <= 0: return
    M = MATS[m]
    n = vnoise(h, w, 3, seed)
    col = M["base"][None, None, :] * (1 - M["noise"] * n[..., None]) + M["dark"][None, None, :] * (M["noise"] * n[..., None])
    if face in ("north", "south", "west", "east") and M["vgrad"]:
        g = np.linspace(1 + M["vgrad"] / 2, 1 - M["vgrad"] / 2, h)[:, None, None]
        col = col * g
    p = M["pattern"]
    if p == "cloth":     # tkanina: pionowe włókna + fałdy
        col *= (0.92 + 0.08 * np.sin(np.arange(w) * 1.7 + seed))[None, :, None]
        col *= (0.9 + 0.1 * vnoise(h, w, 6, seed + 1))[..., None]
    elif p == "hair":    # pasma
        strands = vnoise(1, w, 1, seed)[0]
        col *= (0.75 + 0.35 * strands)[None, :, None]
    elif p == "halftone":  # druk rastrowy (love_note)
        yy, xx = np.mgrid[0:h, 0:w]
        dots = ((xx + yy) % 3 == 0)
        col = np.where(dots[..., None], col * 0.55, col)
    elif p == "skin":
        col *= (0.95 + 0.08 * vnoise(h, w, 2, seed + 2))[..., None]
    elif p == "shadow":  # czerń z mgiełką na krawędziach
        edge = np.minimum.reduce([np.arange(w)[None, :].repeat(h, 0), (w - 1 - np.arange(w))[None, :].repeat(h, 0),
                                  np.arange(h)[:, None].repeat(w, 1), (h - 1 - np.arange(h))[:, None].repeat(w, 1)])
        col = col * (1 + 0.6 * (edge == 0)[..., None])
    col = col * SIDE_SHADE[face]
    col += np.random.default_rng(seed + 9).normal(0, M["grain"], col.shape)
    a = np.full((h, w, 1), M["alpha"], float)
    region = np.concatenate([np.clip(col, 0, 255), a], -1)
    img[y:y + h, x:x + w] = region


def draw_grid(img, rect, grid, pal, flip=False):
    """Nakłada rysunek ASCII (rozciągnięty do rozmiaru ściany) na ścianę."""
    x, y, w, h = [int(round(v)) for v in rect]
    rows = [r for r in grid.strip("\n").split("\n")]
    gh, gw = len(rows), len(rows[0])
    for yy in range(h):
        for xx in range(w):
            c = rows[yy * gh // h][(w - 1 - xx if flip else xx) * gw // w]
            if c in pal:
                v = pal[c]
                img[y + yy, x + xx, :3] = v[:3]
                img[y + yy, x + xx, 3] = v[3] if len(v) > 3 else 255


def P(s, a=255):
    return tuple(hexc(s)) + (a,)


# =================================================================== definicje mobów
MOBS = {}


def mob(id_, height, width, parts, notes):
    MOBS[id_] = dict(parts=parts, height=height, width=width, notes=notes)


# ---- materiały
mat("shadow", "17141a", "050407", noise=0.35, grain=6, vgrad=0.1, pattern="shadow")
mat("shadow_cloak", "121016", "040306", noise=0.45, grain=7, vgrad=0.25, pattern="cloth")
mat("eye_glow", "f8f6ff", "d0d8ff", noise=0.05, grain=2, vgrad=0)
mat("balaclava", "2a2829", "0e0d0e", noise=0.3, grain=8, pattern="halftone")
mat("hoodie", "1f1d22", "0b0a0d", noise=0.3, grain=7, pattern="cloth")
mat("jeans", "2b3446", "161c28", noise=0.3, grain=8, pattern="cloth")
mat("shoe", "141414", "050505", noise=0.2, grain=5)
mat("skin_pale", "d9c3b0", "a88a76", noise=0.25, grain=6, pattern="skin")
mat("skin_warm", "e2b596", "b07f62", noise=0.22, grain=6, pattern="skin")
mat("skin_grey", "a9a49c", "6f6a63", noise=0.3, grain=7, pattern="skin")
mat("skin_mask", "d7cfc2", "9c9385", noise=0.25, grain=6, pattern="skin")
mat("wig", "1c1714", "070504", noise=0.35, grain=6, pattern="hair")
mat("uniform_pink", "d9868e", "9e555d", noise=0.25, grain=7, pattern="cloth")
mat("collar", "efe9e4", "c2b9b2", noise=0.15, grain=5)
mat("sock", "2a2a33", "121218", noise=0.2, grain=6)
mat("sheet", "e9e7e2", "a9a59e", noise=0.4, grain=6, vgrad=0.3, pattern="cloth")
mat("sheet_dark", "c9c6bf", "8a867f", noise=0.45, grain=7, vgrad=0.35, pattern="cloth")
mat("cat_hair", "c3c9bd", "7f877a", noise=0.35, grain=7, pattern="hair")
mat("cat_inner_ear", "d79a96", "a56b67", noise=0.2, grain=5)
mat("sweater", "e6e3c8", "a9a68b", noise=0.3, grain=7, pattern="cloth")
mat("cigar", "6a4224", "3a2212", noise=0.35, grain=7)
mat("ember", "ff5a1a", "c01808", noise=0.3, grain=8, vgrad=0)
mat("black_rag", "151213", "060505", noise=0.4, grain=7, pattern="cloth")
mat("locust_body", "1a1310", "070504", noise=0.45, grain=8, pattern="shadow")
mat("locust_hair", "120e0c", "030202", noise=0.4, grain=6, pattern="hair")
mat("bone_grey", "8d8a84", "55524c", noise=0.35, grain=8, pattern="skin")

# ---- ściany z rysunkiem (16 kolumn = 8 px głowy × 2)
FACE_SHADOW = """
................
................
................
................
................
................
...WW......WW...
...WW......WW...
................
................
................
................
................
................
................
................"""
FACE_LOVE = """
kkkkkkkkkkkkkkkk
kkkkkkkkkkkkkkkk
kkSSSSSSSSSSSSkk
kSSddSSSSSSddSSk
kSdwwbdSSdbwwdSk
kSddwbddddbwddSk
kSSddddSSddddSSk
kSSSSSSnnSSSSSSk
kSSSSSSnnSSSSSSk
kSSSSSSSSSSSSSSk
kTTTTTTTTTTTTTTk
kTTTTTTTTTTTTTTk
kTTTTTTTTTTTTTTk
kkSSSSSSSSSSSSkk
kkkSSSSSSSSSSkkk
kkkkkkkkkkkkkkkk"""
FACE_WIG = """
HHHHHHHHHHHHHHHH
HHHHHHHHHHHHHHHH
HHHhHHHHHHHHhHHH
HHSSHHSSHHSSHHSH
HSSSSSSSSSSSSSSH
HSbbbSSSSSSbbbSH
HSSwbSSSSSSbwSSH
HSSSSSSnSSSSSSSH
HSSSSSSnSSSSSSSH
HSSSSSnnnSSSSSSH
HSSSSSSSSSSSSSSH
HSSSSrrrrrSSSSSH
HSSSSSSSSSSSSSSH
HHSSSSSSSSSSSSHH
HHHSSSSSSSSSSHHH
HHHHSSSSSSSSHHHH"""
FACE_GHOST = """
................
................
................
................
...kkk....kkk...
..kkkkk..kkkkk..
..kkkkk..kkkkk..
...kkk....kkk...
................
................
.......kk.......
......kkkk......
.......kk.......
................
................
................"""
FACE_CAT = """
HHHHHHHHHHHHHHHH
HHHHhHHHHHHhHHHH
HHSSHHSSSSHHSSHH
HSSSSSSSSSSSSSSH
HSddddSSSSddddSH
HSSwkSSSSSSwkSSH
HSSSSSSSSSSSSSSH
HSSSSSSkkSSSSSSH
HSSSSSkddkSSSSSH
HSSSSSSSSSSSSSSH
HSSSSSSSSSSSSSSH
HSSSSSSSSSSSSSSH
HHSSSSSSSSSSSSHH
HHSSSSSSSSSSSSHH
HHHSSSSSSSSSSHHH
HHHHSSSSSSSSHHHH"""
FACE_SCREAMER = """
kkkkkkkkkkkkkkkk
kkkkkMMMMMMkkkkk
kkkMMMMMMMMMMkkk
kkMMMMMMMMMMMMkk
kMMddddMMddddMMk
kMdwwkdMMdkwwdMk
kMdwkkdMMdkkwdMk
kMMddddMMddddMMk
kMMMMMMnnMMMMMMk
kMMMMMMnnMMMMMMk
kMMMMMnnnnMMMMMk
kMMMrrrrrrrrMMMk
kMMMrkkkkkkrMMMk
kMMMMrrrrrrMMMMk
kkMMMMMMMMMMMMkk
kkkkMMMMMMMMkkkk"""
FACE_LOCUST = """
HHHHHHHHHHHHHHHH
HHHHHHHHHHHHHHHH
HHHHSSSSSSSHHHHH
HHHSSSSSSSSSHHHH
HHSSkkSSSkkSSHHH
HHSkwkSSSkwkSHHH
HHSSkkSSSkkSSHHH
HHSSSSSnnSSSSHHH
HHSSSSSnnSSSSHHH
HHSSSSnnnnSSSHHH
HHHSSrrrrrSSHHHH
HHHSSrkkkrSSHHHH
HHHHSSrrrSSHHHHH
HHHHHSSSSSHHHHHH
HHHHHHHHHHHHHHHH
HHHHHHHHHHHHHHHH"""
FACE_HIDDEN = """
kkSSSSSSSSSSSSkk
kSSSSSSSSSSSSSSk
SSSSSSSSSSSSSSSS
SSRRRRSSSSRRRRSS
SRwwwwRSSRwwwwRS
SRwwkwRSSRwkwwRS
SRwwwwRSSRwwwwRS
SSRRRRSSSSRRRRSS
SSSSSSSSSSSSSSSS
SSSSSSSnnSSSSSSS
SSSSSSSSSSSSSSSS
SrrrrrrrrrrrrrrS
SrwkwkwkwkwkwkrS
SSrrrrrrrrrrrrSS
kSSSSSSSSSSSSSSk
kkSSSSSSSSSSSSkk"""
FACE_PAL = {
    "W": P("ffffff"), "k": P("0c0a0b"), "S": None, "d": P("2a1f1c"), "w": P("f2eee6"), "b": P("1a120e"),
    "n": None, "T": P("101010"), "H": None, "h": None, "r": P("5a1418"), "M": None, "R": P("a01820"),
}


def face_pal(skin, hair=None, extra=None):
    pal = {k: v for k, v in FACE_PAL.items() if v is not None}
    s = MATS[skin]["base"]
    pal["n"] = tuple(s * 0.78) + (255,)
    pal["S"] = None
    if hair: pal["H"] = None; pal["h"] = tuple(MATS[hair]["base"] * 1.5) + (255,)
    pal.update(extra or {})
    return {k: v for k, v in pal.items() if v is not None}


def humanoid(head_mat, body_mat, arm_mat, leg_mat, head=(8, 8, 8), body=(8, 12, 4), arm=(4, 12, 4), leg=(4, 12, 4),
             head_faces=None, leg_len=None, arm_len=None, extra_parts=(), body_extra=(), head_extra=()):
    """Humanoid w stylu vanilla; zwraca listę części. Wysokość = nogi + tułów + głowa."""
    hw, hh, hd = head; bw, bh, bd = body; aw, ah, ad = arm; lw, lh, ld = leg
    if leg_len: lh = leg_len
    if arm_len: ah = arm_len
    hip = lh; shoulder = lh + bh
    parts = [
        Part("head", (0, shoulder, 0), [Box((-hw / 2, -hh, -hd / 2), (hw, hh, hd), head_mat, head_faces)] + list(head_extra), "head"),
        Part("body", (0, shoulder, 0), [Box((-bw / 2, 0, -bd / 2), (bw, bh, bd), body_mat)] + list(body_extra), "body"),
        Part("right_arm", (-(bw / 2 + aw / 2), shoulder - 1, 0), [Box((-aw / 2, -1, -ad / 2), (aw, ah, ad), arm_mat)], "right_arm"),
        Part("left_arm", ((bw / 2 + aw / 2), shoulder - 1, 0), [Box((-aw / 2, -1, -ad / 2), (aw, ah, ad), arm_mat)], "left_arm"),
        Part("right_leg", (-bw / 4, hip, 0), [Box((-lw / 2, 0, -ld / 2), (lw, lh, ld), leg_mat)], "right_leg"),
        Part("left_leg", (bw / 4, hip, 0), [Box((-lw / 2, 0, -ld / 2), (lw, lh, ld), leg_mat)], "left_leg"),
    ]
    return parts + list(extra_parts)


# 1. shadow_watcher – agresywny: wysoki cień, świecące oczy (osobna część, pełna jasność)
sw = humanoid("shadow", "shadow_cloak", "shadow", "shadow", head=(8, 9, 7), body=(9, 15, 5), arm=(3, 17, 3), leg=(3, 13, 3),
              head_faces={"north": (FACE_SHADOW, {"W": P("e8e4ff")})},
              body_extra=[Box((-5.5, 9, -3), (11, 8, 6), "shadow_cloak")])
sw.append(Part("eyes", (0, 28, 0), [Box((-2.6, -6.0, -3.6), (1.0, 1.0, 0.1), "eye_glow"), Box((1.6, -6.0, -3.6), (1.0, 1.0, 0.1), "eye_glow")], "head", emissive=True))
mob("shadow_watcher", 37 / 16, 0.75, sw, "Agresywny. Cała postać czarna, rozmyta na krawędziach; płaszcz rozszerza się od pasa. "
    "Oczy to osobna część `eyes` renderowana z pełną jasnością (Brightness 15) i obracana razem z głową.")

# 2. love_note – agresywny: kominiarka, zaklejone usta, bluza, jeansy
mob("love_note", 32 / 16, 0.7, humanoid("balaclava", "hoodie", "hoodie", "jeans",
    head_faces={"north": (FACE_LOVE, face_pal("skin_pale", extra={"S": P("d9c3b0")}))},
    body_extra=[Box((-4.5, -1, -2.5), (9, 4, 5), "hoodie")]),
    "Agresywny. Kominiarka w rastrze jak na wycinku z gazety, czarny pasek taśmy na ustach, ciemne obwódki oczu.")

# 3. wig_guy – agresywny (śmieszny): peruka z długimi włosami, różowy mundurek
wig_back = Box((-4.75, -9.5, 3.6), (9.5, 14, 1.5), "wig")
wig_l = Box((3.6, -9.5, -4.6), (1.4, 12, 9.2), "wig")
wig_r = Box((-5.0, -9.5, -4.6), (1.4, 12, 9.2), "wig")
wig_top = Box((-4.75, -10, -4.75), (9.5, 1.5, 9.5), "wig")
mob("wig_guy", 33 / 16, 0.7, humanoid("skin_warm", "uniform_pink", "uniform_pink", "sock", head=(9, 10, 9),
    head_faces={"north": (FACE_WIG, face_pal("skin_warm", "wig", {"S": P("e2b596"), "H": P("1c1714")}))},
    head_extra=[wig_back, wig_l, wig_r, wig_top], body_extra=[Box((-3, 0, -2.6), (6, 3, 0.6), "collar")]),
    "Agresywny, memiczny. Za duża głowa, czarna peruka do ramion (osobne bryły z tyłu i po bokach), różowy mundurek z białym kołnierzem.")

# 4. sheet_ghost – neutralny: lewitujące prześcieradło z poszarpanym dołem
ghost_parts = [
    Part("body", (0, 22, 0), [Box((-5, -8, -5), (10, 8, 10), "sheet", {"north": (FACE_GHOST, {"k": P("0a0808")})}),
                              Box((-6, 0, -6), (12, 12, 12), "sheet"),
                              Box((-6.5, 12, -6.5), (4, 4, 4), "sheet_dark"), Box((-1.5, 12, -6.2), (4, 6, 3), "sheet_dark"),
                              Box((3, 12, -6), (3.5, 3, 5), "sheet_dark"), Box((-6, 12, 1), (5, 5, 5), "sheet_dark"),
                              Box((1.5, 12, 2), (5, 7, 4.5), "sheet_dark")], "body"),
    Part("right_arm", (-6, 20, 0), [Box((-4, -1, -2), (4, 9, 3), "sheet")], "right_arm"),
    Part("left_arm", (6, 20, 0), [Box((0, -1, -2), (4, 9, 3), "sheet")], "left_arm"),
]
mob("sheet_ghost", 30 / 16, 0.8, ghost_parts,
    "Neutralny. Bez nóg: unosi się 0,3 bloku nad ziemią (sinusoida ±0,08 bloku, okres 2 s). Poszarpane pasy materiału u dołu kołyszą się z tułowiem.")

# 5. cigar_cat – neutralny (śmieszny): kotka z cygarem
cat_ears = [Box((-4, -11, -1), (3, 3, 1.5), "cat_hair", {"north": ("ss\nss", {"s": P("d79a96")})}),
            Box((1, -11, -1), (3, 3, 1.5), "cat_hair", {"north": ("ss\nss", {"s": P("d79a96")})}),
            Box((-4.5, -8.5, -4.5), (9, 3, 9), "cat_hair")]
cat = humanoid("skin_warm", "sweater", "sweater", "jeans", head_faces={"north": (FACE_CAT, face_pal("skin_warm", "cat_hair", {"S": P("f0cfae"), "H": P("c3c9bd")}))},
               head_extra=cat_ears)
cat.append(Part("cigar", (0, 24 + 0, 0), [Box((1.0, -2.5, -10), (2, 2, 6.5), "cigar")], "head"))
cat.append(Part("ember", (0, 24, 0), [Box((1.1, -2.4, -10.15), (1.8, 1.8, 0.2), "ember")], "head", emissive=True))
mob("cigar_cat", 32 / 16, 0.7, cat, "Neutralny, memiczny. Kocie uszy, potargane szare włosy, sweter; cygaro wystaje z ust, "
    "żarzący się koniec to osobna część `ember` z pełną jasnością (co 2–4 s cząsteczka `minecraft:smoke` z końca).")

# 6. hollow_screamer – unholy: zgarbiony, długie ręce, twarz-maska
hs = humanoid("skin_mask", "black_rag", "black_rag", "black_rag", head=(8, 9, 8), body=(9, 13, 5), arm=(3, 24, 3), leg=(3, 15, 3),
              head_faces={"north": (FACE_SCREAMER, face_pal("skin_mask", extra={"M": P("d7cfc2")}))},
              head_extra=[Box((-4.5, -9.5, -1), (9, 9, 5.5), "black_rag")])
for p in hs:
    if p.name in ("right_arm", "left_arm"): p.boxes.append(Box((-1.5, 21, -2.4), (3, 3, 1.2), "skin_grey"))
mob("hollow_screamer", 38 / 16, 0.8, hs, "Unholy. Tułów pochylony stale o 25° do przodu (rola `body` + `hunch`), głowa wysunięta. "
    "Ręce sięgają za kolana, szare dłonie. Twarz to blada maska z wybałuszonymi oczami i otwartymi ustami, reszta głowy w czarnym kapturze.")
for p in hs:
    if p.name == "body": p.extra["hunch_deg"] = 25

# 7. locust – unholy: wychudzone szczudła, ludzka twarz, wielka czarna czupryna
loc = humanoid("skin_warm", "locust_body", "locust_body", "locust_body", head=(7, 8, 7), body=(6, 11, 4), arm=(2, 27, 2), leg=(2, 27, 2),
               head_faces={"north": (FACE_LOCUST, face_pal("skin_warm", "locust_hair", {"S": P("d9a888"), "H": P("120e0c")}))},
               head_extra=[Box((-5.5, -10, -2), (11, 6, 8), "locust_hair"), Box((-6, -6, 0), (2.5, 12, 5), "locust_hair"),
                           Box((3.5, -6, 0), (2.5, 14, 5), "locust_hair"), Box((-4, -2, 2.5), (8, 9, 2), "locust_hair")],
               body_extra=[Box((-1, 11, -1), (2, 3, 2), "locust_body")])
for p in loc:
    if p.name == "body": p.boxes.append(Box((-2.5, 3, -2.4), (5, 4, 0.6), "bone_grey"))
mob("locust", 46 / 16, 0.6, loc, "Unholy. Bardzo długie, cienkie nogi i ręce, malutki tułów z prześwitującymi żebrami, "
    "ludzka twarz z grymasem w burzy czarnych włosów, które opadają na ramiona.")

# 8. hidden_face – NOWY neutralny (DECISIONS #38): atakuje po spojrzeniu w oczy
hf = humanoid("skin_grey", "black_rag", "skin_grey", "black_rag", head=(8, 9, 8), body=(7, 14, 4), arm=(3, 18, 3), leg=(3, 15, 3),
              head_faces={"north": (FACE_HIDDEN, face_pal("skin_grey", extra={"S": P("b8b3aa")}))})
for p in hf:
    if p.name in ("right_arm", "left_arm"):
        p.boxes.append(Box((-2, 15, -2), (4, 5, 4), "skin_grey"))   # duże dłonie
        p.extra["cover_face"] = True
mob("hidden_face", 37 / 16, 0.65, hf, "NOWY neutralny slop (DECISIONS #38). Wychudzona, szara postać, która **zasłania twarz dużymi dłońmi**. "
    "Gdy gracz patrzy na jej głowę (jak u Endermana) dłużej niż 1 s, opuszcza ręce i odsłania przekrwione oczy i zęby, "
    "po 1 s rusza do ataku. Jeśli gracz odwróci wzrok wcześniej, zasłania się z powrotem.")


# =================================================================== eksport
def element(b):
    ox, oy, oz = b.o; w, h, d = b.s
    fr = [8 + ox / 2, 8 - (oy + h) / 2, 8 + oz / 2]
    to = [8 + (ox + w) / 2, 8 - oy / 2, 8 + (oz + d) / 2]
    faces = {}
    for f, (x, y, ww, hh) in face_rects(b).items():
        faces[f] = {"uv": [round(x * 16 / TEX, 4), round(y * 16 / TEXH, 4), round((x + ww) * 16 / TEX, 4), round((y + hh) * 16 / TEXH, 4)],
                    "texture": "#t", "tintindex": 0}
    return {"from": [round(v, 4) for v in fr], "to": [round(v, 4) for v in to], "faces": faces}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, separators=(",", ":")) + "\n")


def export(mid, m):
    global TEXH
    boxes = [b for p in m["parts"] for b in p.boxes]
    TEXH = pack(boxes)
    img = np.zeros((TEXH, TEX, 4), float)
    for i, b in enumerate(boxes):
        for f, r in face_rects(b).items():
            paint_face(img, r, b.mat, f, seed=hash((mid, i, f)) % 100000)
        for f, (grid, pal) in b.faces.items():
            draw_grid(img, face_rects(b)[f], grid, pal)
    tex = Image.fromarray(img.astype(np.uint8))
    (A / "textures/entity/mob").mkdir(parents=True, exist_ok=True)
    tex.save(A / f"textures/entity/mob/{mid}.png", optimize=True)
    hal = img.copy(); hal[..., :3] = 0; hal[..., 3] *= 0.6
    Image.fromarray(hal.astype(np.uint8)).save(A / f"textures/entity/mob/{mid}_hallucination.png", optimize=True)
    spec = {"id": mid, "height": round(m["height"], 3), "width": m["width"], "scale": 2.0, "notes": m["notes"], "parts": []}
    for p in m["parts"]:
        for var, texname in (("", mid), ("_hallucination", mid + "_hallucination")):
            write_json(A / f"models/entity_part/{mid}_{p.name}{var}.json",
                       {"textures": {"t": f"unholymc:entity/mob/{texname}", "particle": f"unholymc:entity/mob/{texname}"},
                        "elements": [element(b) for b in p.boxes]})
        ref = lambda v: {"type": "minecraft:model", "model": f"unholymc:entity_part/{mid}_{p.name}{v}",
                         "tints": [{"type": "minecraft:custom_model_data", "index": 0, "default": -1}]}
        write_json(A / f"items/mob/{mid}/{p.name}.json", {"model": {"type": "minecraft:select", "property": "minecraft:custom_model_data",
                   "index": 0, "cases": [{"when": "hallucination", "model": ref("_hallucination")}], "fallback": ref("")}})
        spec["parts"].append({"name": p.name, "pivot": [round(p.pivot[0] / 16, 4), round(p.pivot[1] / 16, 4), round(p.pivot[2] / 16, 4)],
                              "role": p.role, "emissive": p.emissive, **p.extra})
    DATA.mkdir(parents=True, exist_ok=True)
    (DATA / f"{mid}.json").write_text(json.dumps(spec, indent=1, ensure_ascii=False) + "\n")
    return tex


# =================================================================== podgląd (rzut ortogonalny przód / bok)
def render(m, tex, view="front", scale=6):
    texa = np.asarray(tex)
    H = int(m["height"] * 16 + 8) * scale; W = 48 * scale
    canvas = np.zeros((H, W, 4), np.uint8)
    zbuf = np.full((H, W), -1e9)
    for p in m["parts"]:
        pivx, pivy, pivz = p.pivot
        hunch = math.radians(p.extra.get("hunch_deg", 0))
        for b in p.boxes:
            ox, oy, oz = b.o; w, h, d = b.s
            rects = face_rects(b)
            if view == "front":   # patrzymy od -Z (przód = north)
                fx0, fy0, fw, fh = rects["north"]
                for ty in range(int(fh)):
                    for tx in range(int(fw)):
                        c = texa[int(fy0) + ty, int(fx0) + tx]
                        if c[3] < 10: continue
                        x = pivx + ox + (tx + 0.5) / K; ylocal = oy + (ty + 0.5) / K
                        zlocal = oz
                        yw = pivy - (ylocal * math.cos(hunch) - zlocal * math.sin(hunch)) if hunch else pivy - ylocal
                        sx = int((x + 24) * scale); sy = int(H - 4 * scale - yw * scale)
                        depth = -(pivz + oz)
                        for yy in range(sy, sy + math.ceil(scale / K)):
                            for xx in range(sx, sx + math.ceil(scale / K)):
                                if 0 <= yy < H and 0 <= xx < W and depth > zbuf[yy, xx]:
                                    zbuf[yy, xx] = depth; canvas[yy, xx] = c
            else:                 # bok: patrzymy od -X (ściana west), oś pozioma = z
                fx0, fy0, fw, fh = rects["west"]
                for ty in range(int(fh)):
                    for tx in range(int(fw)):
                        c = texa[int(fy0) + ty, int(fx0) + tx]
                        if c[3] < 10: continue
                        z = pivz + oz + (tx + 0.5) / K; ylocal = oy + (ty + 0.5) / K
                        yw = pivy - (ylocal * math.cos(hunch) - (oz + (tx + 0.5) / K) * math.sin(hunch)) if hunch else pivy - ylocal
                        zz = z - (ylocal * math.sin(hunch) if hunch else 0)
                        sx = int((zz + 24) * scale); sy = int(H - 4 * scale - yw * scale)
                        depth = -(pivx + ox)
                        for yy in range(sy, sy + math.ceil(scale / K)):
                            for xx in range(sx, sx + math.ceil(scale / K)):
                                if 0 <= yy < H and 0 <= xx < W and depth > zbuf[yy, xx]:
                                    zbuf[yy, xx] = depth; canvas[yy, xx] = c
    return Image.fromarray(canvas)


def main():
    tiles = []
    for mid, m in MOBS.items():
        tex = export(mid, m)
        tiles.append((mid, render(m, tex, "front"), render(m, tex, "side")))
    hmax = max(t[1].height for t in tiles)
    sheet = Image.new("RGBA", (sum(t[1].width + t[2].width // 2 + 10 for t in tiles), hmax + 10), (62, 78, 60, 255))
    x = 0
    for mid, f, s in tiles:
        sheet.alpha_composite(f, (x, hmax - f.height + 5))
        s2 = s.resize((s.width // 2, s.height // 2), Image.NEAREST)
        sheet.alpha_composite(s2, (x + f.width - 40, hmax - s2.height + 5))
        x += f.width + s.width // 2 + 10
    sheet.save(PREV / "mobs_preview.png")
    tex_sheet = Image.new("RGBA", (TEX * 4 + 30, 256 * 2 + 10), (40, 40, 44, 255))
    for i, mid in enumerate(MOBS):
        tex_sheet.alpha_composite(Image.open(A / f"textures/entity/mob/{mid}.png"), ((i % 4) * (TEX + 10), (i // 4) * (256 + 10)))
    tex_sheet.save(PREV / "mobs_textures_preview.png")
    print("ok:", ", ".join(MOBS))


if __name__ == "__main__":
    main()
