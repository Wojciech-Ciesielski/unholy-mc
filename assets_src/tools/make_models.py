"""Generuje modele itemów (models/item) i definicje itemów (items/) dla tekstur z make_items.py."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc"
NS = "unholymc"

def w(rel, obj):
    p = A / rel; p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n")

def model(name, layers, parent="minecraft:item/generated"):
    w(f"models/item/{name}.json", {"parent": parent,
      "textures": {f"layer{i}": f"{NS}:item/{l}" for i, l in enumerate(layers)}})

def ref(name):
    return {"type": "minecraft:model", "model": f"{NS}:item/{name}"}

# proste
# woda święcona: identyczna butelka jak vanilla (tekstury minecraft), płyn barwiony tintem warstwy 0
HOLY_TINT = 0x9CE4FF
for n, bottle in (("holy_water", "minecraft:item/potion"), ("splash_holy_water", "minecraft:item/splash_potion")):
    w(f"models/item/{n}.json", {"parent": "minecraft:item/generated",
      "textures": {"layer0": "minecraft:item/potion_overlay", "layer1": bottle}})
    w(f"items/{n}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{n}",
      "tints": [{"type": "minecraft:constant", "value": HOLY_TINT}]}})

for n in ("crucifix", "nightmare_gem"):
    model(n, [n]); w(f"items/{n}.json", {"model": ref(n)})

# latarka: custom_model_data.strings[0] = rdzeń, flags[0] = włączona
cores = ["none", "lapis", "redstone", "emerald", "diamond", "netherite", "nether_star", "nightmare_gem"]
def fl(core):
    beam = "flashlight_beam_dark" if core == "nightmare_gem" else "flashlight_beam"
    model(f"flashlight_{core}", [f"flashlight_{core}"], "minecraft:item/handheld")
    model(f"flashlight_{core}_on", [f"flashlight_{core}", beam], "minecraft:item/handheld")
    return {"type": "minecraft:condition", "property": "minecraft:custom_model_data", "index": 0,
            "on_true": ref(f"flashlight_{core}_on"), "on_false": ref(f"flashlight_{core}")}
cases = [{"when": c, "model": fl(c)} for c in cores if c != "none"]
w("items/flashlight.json", {"model": {"type": "minecraft:select", "property": "minecraft:custom_model_data",
                                      "index": 0, "cases": cases, "fallback": fl("none")}})

# Unholy Sword: custom_model_data.flags[0] = rozładowany
model("unholy_sword", ["unholy_sword"], "minecraft:item/handheld")
model("unholy_sword_discharged", ["unholy_sword_discharged"], "minecraft:item/handheld")
w("items/unholy_sword.json", {"model": {"type": "minecraft:condition", "property": "minecraft:custom_model_data",
    "index": 0, "on_true": ref("unholy_sword_discharged"), "on_false": ref("unholy_sword")}})

# termos: custom_model_data.strings[0] = płyn
fills = ["water", "holy_water", "honey", "milk"]
for f in ["empty"] + fills: model(f"thermos_{f}", [f"thermos_{f}"])
w("items/thermos.json", {"model": {"type": "minecraft:select", "property": "minecraft:custom_model_data",
    "index": 0, "cases": [{"when": f, "model": ref(f"thermos_{f}")} for f in fills],
    "fallback": ref("thermos_empty")}})
print("ok")
