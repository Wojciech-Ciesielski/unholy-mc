"""Paczki zasobów dla klienta vanilla:
 1) nakładka Krwawego Księżyca  -> src/main/resources/resourcepacks/blood_moon/   (serwer dopycha ją na czas eventu)
 2) szarszy dzień (DECISIONS #43) -> src/main/resources/assets/minecraft/textures/colormap/ (stała część głównej paczki)

Kolormapy są pochodną vanilla (assets/minecraft/textures/colormap/*.png z jara klienta 26.2). Ścieżkę do rozpakowanego
jara podaj w VANILLA=/…/assets/minecraft (domyślnie assets_src/audio_src/../vanilla, poza repo).
"""
import json, math, os
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
VAN = Path(os.environ.get("VANILLA", ROOT / "assets_src/vanilla/assets/minecraft"))
BM = ROOT / "src/main/resources/resourcepacks/blood_moon"
MAIN = ROOT / "src/main/resources/assets/minecraft"
PACK_FORMAT = 88  # 26.2


def w(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


# ------------------------------------------------------------------ 1) Krwawy Księżyc
w(BM / "pack.mcmeta", json.dumps({"pack": {"description": "The Unholy Minecraft – Blood Moon",
                                           "min_format": PACK_FORMAT, "max_format": PACK_FORMAT}}, indent=2) + "\n")

RED = """
vec3 unholy_blood(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    return vec3(0.09 + l * 1.35, l * 0.22, l * 0.20);
}
"""
# mgła: ta sama logika co vanilla fog.glsl, ale kolor mgły zabarwiony na krwisto (działa na teren, moby, niebo przy horyzoncie)
w(BM / "assets/minecraft/shaders/include/fog.glsl", """#version 330

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};
""" + RED + """
float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }
    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    return max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    // Krwawy Księżyc: lekki czerwony odcień całego obrazu + krwista mgła
    vec3 base = mix(inColor.rgb, inColor.rgb * vec3(1.10, 0.82, 0.80), 0.8);
    return vec4(mix(base, unholy_blood(fogColor.rgb), fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}
""")
w(BM / "assets/minecraft/shaders/core/sky.fsh", """#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

void main() {
    vec4 sky = vec4(unholy_blood(ColorModulator.rgb) * 0.9, ColorModulator.a);
    fragColor = apply_fog(sky, sphericalVertexDistance, cylindricalVertexDistance, 0.0, FogSkyEnd, FogSkyEnd, FogSkyEnd, FogColor);
}
""")
w(BM / "assets/minecraft/shaders/core/stars.fsh", """#version 330

#moj_import <minecraft:dynamictransforms.glsl>

out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb * vec3(1.0, 0.35, 0.30), ColorModulator.a * 0.6);
}
""")
w(BM / "assets/minecraft/shaders/core/rendertype_clouds.fsh", """#version 330

#moj_import <minecraft:fog.glsl>

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vec4(unholy_blood(vertexColor.rgb) * 0.8, vertexColor.a);
    color.a *= 1.0f - linear_fog_value(vertexDistance, 0, FogCloudsEnd);
    fragColor = color;
}
""")

# księżyc: większa tarcza (12x12 zamiast 8x8) w formacie vanilla full_moon.png
MOON = """
....aabbaa..
..abbcbbdba.
.abbbcddbbba
abdbbbbcbbda
abbdcbbbbdba
bbcbbdbdbbcb
bbbdbbbbcbbb
abbbcbbdbbba
abdbbbcbbdba
.abcbbbbbba.
..aabbdbaa..
....aabb...."""
PAL = {"a": (138, 26, 20), "b": (200, 40, 28), "c": (255, 90, 58), "d": (106, 12, 10)}
img = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
for y in range(32):
    for x in range(32):
        k = max(0.0, 1 - math.hypot(x - 15.5, y - 15.5) / 16.5) ** 1.6
        img.putpixel((x, y), (int(170 * k), int(12 * k), int(8 * k), 255))
for y, r in enumerate(MOON.strip("\n").split("\n")):
    for x, ch in enumerate(r):
        if ch != ".": img.putpixel((10 + x, 10 + y), PAL[ch] + (255,))
p = BM / "assets/minecraft/textures/environment/celestial/moon/full_moon.png"
p.parent.mkdir(parents=True, exist_ok=True); img.save(p)


# ------------------------------------------------------------------ kolormapy: krwawe (nakładka) i wyblakłe (główna paczka)
def recolor(src, fn, dst):
    a = np.asarray(Image.open(src).convert("RGBA")).astype(float)
    rgb = fn(a[..., :3])
    out = np.dstack([np.clip(rgb, 0, 255), a[..., 3]]).astype(np.uint8)
    dst.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(out).save(dst, optimize=True)


def desat(rgb, amount):
    l = rgb @ np.array([0.299, 0.587, 0.114])
    return rgb * (1 - amount) + l[..., None] * amount


if VAN.exists():
    for name in ("grass", "foliage", "dry_foliage"):
        src = VAN / f"textures/colormap/{name}.png"
        # szarszy dzień: −30% nasycenia, −6% jasności
        recolor(src, lambda c: desat(c, 0.30) * 0.94, MAIN / f"textures/colormap/{name}.png")
        # Krwawy Księżyc: ciemne, rdzawo-bordowe liście i trawa
        recolor(src, lambda c: (lambda l: np.stack([40 + l * 0.75, l * 0.22, l * 0.18], -1))(c @ np.array([0.299, 0.587, 0.114])),
                BM / f"assets/minecraft/textures/colormap/{name}.png")
else:
    print("Brak rozpakowanych tekstur vanilla w", VAN, "– pomijam kolormapy")
# ------------------------------------------------------------------ 3) lekkie shadery atmosfery (główna paczka, DECISIONS #43)
# Koszt: kilka operacji na piksel w shaderach, które i tak liczą mgłę – bez dodatkowych przebiegów (post-processu).
w(MAIN / "shaders/include/fog.glsl", """#version 330

// The Unholy Minecraft: vanilla 26.2 fog.glsl + lekka atmosfera:
//  - korekcja kolorów świata: -20% nasycenia, chłodniej (szarawy dzień),
//  - noc: głębsze cienie (jasne źródła światła zostają jasne),
//  - delikatna mgiełka zaczynająca się bliżej niż w vanilla.
// Działa tylko tam, gdzie gra rysuje mgłę (FogColor.a > 0), czyli w świecie; GUI i przedmioty w ekwipunku bez zmian.
// Podczas Krwawego Księżyca ten plik zastępuje wersja z nakładki resourcepacks/blood_moon.

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

vec3 unholy_grade(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    vec3 g = mix(vec3(l), c, 0.80) * vec3(0.97, 1.0, 1.03);
    // jasność mgły ~ pora dnia: jasna mgła = dzień, ciemna = noc
    float day = clamp(dot(FogColor.rgb, vec3(0.3333)) * 1.8, 0.0, 1.0);
    vec3 night = pow(max(g, vec3(0.0)), vec3(1.35));
    return mix(night, g, 0.25 + 0.75 * day);
}

float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }
    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    float vanilla = max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
    float haze = linear_fog_value(cylindricalVertexDistance, renderDistanceEnd * 0.35, renderDistanceEnd) * 0.35;
    return max(vanilla, haze);
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    vec3 base = inColor.rgb;
    vec3 fogRgb = fogColor.rgb;
    if (fogColor.a > 0.0) {
        base = unholy_grade(base);
        fogRgb = mix(vec3(dot(fogRgb, vec3(0.299, 0.587, 0.114))), fogRgb, 0.75);
    }
    return vec4(mix(base, fogRgb, fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}
""")

# winieta: vanilla rysuje textures/misc/vignette.png zawsze (mnożenie: jasny piksel = ciemniej), my ją wzmacniamy
yy, xx = np.mgrid[0:256, 0:256]
d = np.hypot(xx - 127.5, yy - 127.5) / 181.0          # 0 w środku, 1 w rogu
v = np.clip((d - 0.38) / 0.62, 0, 1) ** 1.6 * 215
vig = Image.fromarray(v.astype(np.uint8), "L").convert("RGB")
(MAIN / "textures/misc").mkdir(parents=True, exist_ok=True)
vig.save(MAIN / "textures/misc/vignette.png")
print("ok")
