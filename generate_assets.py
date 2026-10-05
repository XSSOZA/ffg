#!/usr/bin/env python3
"""Generates: WeaponDefs.java, 32x32 textures, item models, item definitions, lang, recipes.
Run again any time you edit the lists below:  python3 generate_assets.py
Replace any texture in assets/arsenal/textures/item/ with your own art (keep the file name)."""
import json, math, os, random
from PIL import Image, ImageDraw

NS = "arsenal"
ROOT = os.path.dirname(os.path.abspath(__file__))
RES = f"{ROOT}/src/main/resources"
JAVA = f"{ROOT}/src/main/java/com/arsenal"
SIZE = 32

ELEMENTS = ["FIRE", "ICE", "BLOOD", "LIGHTNING", "SHADOW", "HOLY", "WIND", "POISON", "VOID"]
TYPES = ["KATANA", "GREATSWORD", "SCYTHE", "WAND", "BOW", "DAGGER"]
TIERS = ["UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"]

ELEM_COLOR = {  # main, bright
    "FIRE": ((255, 110, 20), (255, 220, 90)), "ICE": ((90, 190, 255), (220, 250, 255)),
    "BLOOD": ((190, 15, 35), (255, 90, 100)), "LIGHTNING": ((250, 230, 60), (255, 255, 200)),
    "SHADOW": ((90, 50, 140), (190, 120, 255)), "HOLY": ((255, 235, 150), (255, 255, 240)),
    "WIND": ((120, 230, 190), (225, 255, 245)), "POISON": ((110, 220, 50), (200, 255, 120)),
    "VOID": ((140, 40, 200), (230, 100, 255)),
}
PREFIX = {"FIRE": "Crimson Flare", "ICE": "Frostveil", "BLOOD": "Bloodmoon", "LIGHTNING": "Stormcall",
          "SHADOW": "Umbral", "HOLY": "Radiant", "WIND": "Tempest", "POISON": "Venomfang", "VOID": "Voidwalker"}
NOUN = {"KATANA": "Katana", "GREATSWORD": "Greatsword", "SCYTHE": "Scythe", "WAND": "Wand", "BOW": "Bow", "DAGGER": "Dagger"}
SKILL_E = {"FIRE": "Blazing", "ICE": "Glacial", "BLOOD": "Crimson", "LIGHTNING": "Thunder", "SHADOW": "Eclipse",
           "HOLY": "Judgement", "WIND": "Cyclone", "POISON": "Plague", "VOID": "Abyssal"}
SKILL_T = {"KATANA": "Iaido Wave", "GREATSWORD": "Heavens Cleave", "SCYTHE": "Reaper Quake", "WAND": "Arcane Beam",
           "BOW": "Starfall Volley", "DAGGER": "Phantom Step"}
ELEM_ITEM = {"FIRE": "minecraft:blaze_rod", "ICE": "minecraft:packed_ice", "BLOOD": "minecraft:redstone_block",
             "LIGHTNING": "minecraft:lightning_rod", "SHADOW": "minecraft:echo_shard", "HOLY": "minecraft:gold_block",
             "WIND": "minecraft:phantom_membrane", "POISON": "minecraft:spider_eye", "VOID": "minecraft:ender_eye"}
TIER_MAT = {"UNCOMMON": "minecraft:iron_ingot", "RARE": "minecraft:gold_ingot", "EPIC": "minecraft:diamond",
            "LEGENDARY": "minecraft:netherite_ingot", "MYTHIC": "minecraft:nether_star"}

# (id, display name, type, element, tier, skill)  -- Souls-like specials
SPECIAL = [
    ("ashen_knights_greatsword", "Ashen Knight's Greatsword", "GREATSWORD", "FIRE", "MYTHIC", "Cinder Execution"),
    ("hollow_moon_curved_blade", "Hollow Moon Curved Blade", "KATANA", "SHADOW", "LEGENDARY", "Moonlit Dance"),
    ("lord_of_cinder_halberd", "Lord of Cinder's Halberd", "SCYTHE", "HOLY", "MYTHIC", "Sunlit Sovereign"),
]

weapons = []
for ti, t in enumerate(TYPES):
    for ei, e in enumerate(ELEMENTS):
        tier = TIERS[(ti + ei * 2) % 5]
        name = f"{PREFIX[e]} {NOUN[t]}"
        wid = name.lower().replace(" ", "_")
        weapons.append((wid, name, t, e, tier, f"{SKILL_E[e]} {SKILL_T[t]}"))
weapons += SPECIAL
assert len(weapons) == 57, len(weapons)

# ----------------------------------------------------------------- drawing
def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def blade_mask(p0, p1, w0, w1):
    m = Image.new("L", (SIZE, SIZE), 0)
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dy); nx, ny = -dy / L, dx / L
    poly = [(p0[0] + nx * w0, p0[1] + ny * w0), (p1[0] + nx * w1, p1[1] + ny * w1),
            (p1[0] - nx * w1, p1[1] - ny * w1), (p0[0] - nx * w0, p0[1] - ny * w0)]
    ImageDraw.Draw(m).polygon(poly, fill=255)
    return m

def paint_blade(img, p0, p1, w0, w1, base, light, elem_main, elem_bright):
    m = blade_mask(p0, p1, w0, w1)
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]; L2 = dx * dx + dy * dy
    nx, ny = -dy / math.sqrt(L2), dx / math.sqrt(L2)
    px = img.load(); mp = m.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if mp[x, y] > 0:
                t = ((x - p0[0]) * dx + (y - p0[1]) * dy) / L2
                side = (x - p0[0]) * nx + (y - p0[1]) * ny   # across the blade
                w = w0 + (w1 - w0) * t
                c = mix(base, light, max(0, min(1, t)))
                if abs(side) < 0.55:                       # fuller / core in element colour
                    c = mix(elem_main, elem_bright, t)
                elif side > w * 0.55:                      # bright edge
                    c = mix(light, (255, 255, 255), 0.6)
                elif side < -w * 0.55:                     # shadowed edge
                    c = mix(base, (20, 20, 30), 0.35)
                px[x, y] = c + (255,)

def line(d, a, b, c, w=1):
    d.line([a, b], fill=c + (255,), width=w)

def outline(img):
    px = img.load(); src = img.copy().load()
    for y in range(SIZE):
        for x in range(SIZE):
            if src[x, y][3] == 0:
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx_, ny_ = x + ox, y + oy
                    if 0 <= nx_ < SIZE and 0 <= ny_ < SIZE and src[nx_, ny_][3] > 0:
                        px[x, y] = (18, 14, 24, 255); break

def sparkles(img, e, tier, seed):
    n = {"UNCOMMON": 0, "RARE": 3, "EPIC": 6, "LEGENDARY": 10, "MYTHIC": 15}[tier]
    rnd = random.Random(seed); px = img.load(); main, bright = ELEM_COLOR[e]
    placed = 0
    while placed < n:
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        if px[x, y][3] == 0:
            px[x, y] = (bright if rnd.random() < 0.5 else main) + (255,); placed += 1

def draw_weapon(t, e, tier, seed):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    main, bright = ELEM_COLOR[e]
    steel = mix((150, 158, 172), main, 0.30); light = mix((225, 232, 245), bright, 0.25)
    wrap = (70, 45, 35); wrap2 = (110, 70, 50); gold = (225, 180, 70)
    if t == "KATANA":
        line(d, (2, 29), (8, 23), wrap, 2)
        for i in range(0, 6, 2): img.putpixel((2 + i, 29 - i), wrap2 + (255,))
        d.ellipse((7, 21, 11, 25), fill=gold + (255,))
        paint_blade(img, (10, 22), (29, 3), 1.7, 0.6, steel, light, main, bright)
    elif t == "GREATSWORD":
        line(d, (2, 29), (8, 23), wrap, 2)
        line(d, (5, 19), (13, 27), gold, 2)
        d.rectangle((3, 28, 4, 29), fill=gold + (255,))
        paint_blade(img, (11, 21), (29, 3), 4.2, 1.2, steel, light, main, bright)
    elif t == "DAGGER":
        line(d, (6, 26), (10, 22), wrap, 2)
        line(d, (8, 20), (13, 25), gold, 2)
        paint_blade(img, (11, 21), (25, 7), 2.4, 0.5, steel, light, main, bright)
    elif t == "SCYTHE":
        line(d, (3, 29), (22, 10), wrap, 2)
        for i in range(0, 17, 4): img.putpixel((3 + i, 29 - i), wrap2 + (255,))
        crescent = Image.new("L", (SIZE, SIZE), 0); cd = ImageDraw.Draw(crescent)
        cd.ellipse((4, -2, 29, 21), fill=255); cd.ellipse((8, 2, 33, 25), fill=0)
        cm = crescent.load(); px = img.load()
        for y in range(SIZE):
            for x in range(SIZE):
                if cm[x, y] > 0:
                    tt = (x + (SIZE - y)) / (2 * SIZE)
                    px[x, y] = (mix(steel, bright, 0.25 + 0.5 * (1 - tt)) if (x + y) % 5 else mix(main, bright, 0.5)) + (255,)
        d.ellipse((21, 9, 24, 12), fill=gold + (255,))
    elif t == "WAND":
        line(d, (3, 29), (21, 11), wrap, 2)
        line(d, (14, 18), (17, 15), gold, 3)
        d.ellipse((19, 3, 28, 12), fill=main + (255,)); d.ellipse((21, 5, 26, 10), fill=bright + (255,))
        img.putpixel((22, 6), (255, 255, 255, 255))
        for a, b in (((23, 0), (23, 2)), ((30, 7), (28, 7)), ((17, 7), (19, 7))): line(d, a, b, bright)
    elif t == "BOW":
        pts = []
        for i in range(25):
            u = i / 24; x = (1 - u) ** 2 * 28 + 2 * (1 - u) * u * 3 + u * u * 5
            y = (1 - u) ** 2 * 5 + 2 * (1 - u) * u * 3 + u * u * 28
            pts.append((round(x), round(y)))
        d.line(pts, fill=wrap2 + (255,), width=3)
        d.line(pts[6:19], fill=gold + (255,), width=3)
        line(d, (28, 5), (5, 28), light)
        line(d, (12, 21), (22, 11), main, 1); d.polygon([(23, 10), (26, 7), (21, 7)], fill=bright + (255,))
    outline(img); sparkles(img, e, tier, seed)
    return img

# ------------------------------------------------------------- output files
DISPLAY_SCALE = {"KATANA": 1.25, "GREATSWORD": 1.6, "SCYTHE": 1.75, "WAND": 1.05, "BOW": 1.35, "DAGGER": 0.95}
# per-type held stance (rotation) so they are not held like a plain item
STANCE = {"KATANA": [0, -90, 38], "GREATSWORD": [0, -90, 62], "SCYTHE": [0, -100, 70],
          "WAND": [0, -90, 30], "BOW": [-10, -90, 45], "DAGGER": [8, -90, 50]}

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: f.write(content)

lang = {"itemGroup.arsenal": "Anime Arsenal"}
java_rows = []
sheet = Image.new("RGBA", (SIZE * 10, SIZE * 6), (60, 60, 70, 255))
for i, (wid, name, t, e, tier, skill) in enumerate(weapons):
    img = draw_weapon(t, e, tier, seed=i * 7 + 3)
    os.makedirs(f"{RES}/assets/{NS}/textures/item", exist_ok=True)
    img.save(f"{RES}/assets/{NS}/textures/item/{wid}.png")
    sheet.alpha_composite(img, ((i % 10) * SIZE, (i // 10) * SIZE))
    s = DISPLAY_SCALE[t]; rot = STANCE[t]
    model = {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{wid}"},
             "display": {
                 "thirdperson_righthand": {"rotation": rot, "translation": [0, 3.5, 1.5], "scale": [s * 0.85] * 3},
                 "thirdperson_lefthand": {"rotation": rot, "translation": [0, 3.5, 1.5], "scale": [s * 0.85] * 3},
                 "firstperson_righthand": {"rotation": [0, -90, rot[2] - 10], "translation": [1.1, 3.0, 2.0], "scale": [s * 0.68] * 3},
                 "firstperson_lefthand": {"rotation": [0, 90, -(rot[2] - 10)], "translation": [1.1, 3.0, 2.0], "scale": [s * 0.68] * 3},
                 "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                 "gui": {"scale": [1, 1, 1]}, "fixed": {"rotation": [0, 180, 0]}}}
    write(f"{RES}/assets/{NS}/models/item/{wid}.json", json.dumps(model, indent=2))
    write(f"{RES}/assets/{NS}/items/{wid}.json", json.dumps({"model": {"type": "minecraft:model", "model": f"{NS}:item/{wid}"}}, indent=2))
    lang[f"item.{NS}.{wid}"] = name
    recipe = {"type": "minecraft:crafting_shaped", "category": "equipment",
              "key": {"M": TIER_MAT[tier], "E": ELEM_ITEM[e], "S": "minecraft:stick"},
              "pattern": [" ME", " M ", "S  "], "result": {"id": f"{NS}:{wid}", "count": 1}}
    write(f"{RES}/data/{NS}/recipe/{wid}.json", json.dumps(recipe, indent=2))
    java_rows.append(f'        new WeaponDef("{wid}", WType.{t}, Element.{e}, Tier.{tier}, "{skill}")')

sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(f"{ROOT}/preview_sheet.png")
write(f"{RES}/assets/{NS}/lang/en_us.json", json.dumps(lang, indent=2, ensure_ascii=False))
write(f"{JAVA}/WeaponDefs.java",
      "package com.arsenal;\n\n/** GENERATED by generate_assets.py - do not edit by hand. */\npublic final class WeaponDefs {\n"
      "    private WeaponDefs() {}\n\n    public static final WeaponDef[] ALL = {\n" + ",\n".join(java_rows) + "\n    };\n}\n")
print(len(weapons), "weapons generated")
