#!/usr/bin/env python3
"""Vanilla kahin senaryo tablosunu üretir (scenarios.json).

Her senaryo kendi hücresinde kurulur; koordinatlar hücre merkezine göredir: +x doğu, -x batı, +z güney, -z kuzey.
`place` ilk tick'te, `then` 10 tick sonra uygulanır, `read` 20 tick sonra okunur. Soru etiketleri
BLOK_VERISI_26_50_TODO.md bölüm 6.3'teki S1–S11 ile aynıdır.
"""
import json
from pathlib import Path

CENTER = [0, 0, 0]
SIDES = {"north": [0, 0, -1], "east": [1, 0, 0], "south": [0, 0, 1], "west": [-1, 0, 0]}
scenarios = []


def block(at, name, **states):
    entry = {"at": at, "name": name}
    if states:
        entry["states"] = {k.replace("__", ":"): v for k, v in states.items()}
    return entry


def add(sid, question, place, read=None, then=None, note=None):
    scenario = {"id": sid, "question": question, "place": place, "read": read or [p["at"] for p in place]}
    if then:
        scenario["then"] = then
    if note:
        scenario["note"] = note
    scenarios.append(scenario)


# S1 — çit ve çit kapısı: kapının yönüne ve açık/kapalı durumuna göre bağlantı
for facing in SIDES:
    for open_bit in (False, True):
        add(f"S1/fence_gate/{facing}/{'open' if open_bit else 'closed'}", "S1", [
            block(CENTER, "minecraft:oak_fence"),
            block(SIDES["east"], "minecraft:fence_gate", minecraft__cardinal_direction=facing, open_bit=open_bit),
        ])
for facing in ("north", "east"):
    add(f"S7/pane_fence_gate/{facing}", "S7", [
        block(CENTER, "minecraft:glass_pane"),
        block(SIDES["east"], "minecraft:fence_gate", minecraft__cardinal_direction=facing),
    ])

# S2 / S3 — çit aileleri
add("S2/nether_brick_oak", "S2", [block(CENTER, "minecraft:nether_brick_fence"), block(SIDES["east"], "minecraft:oak_fence")])
add("S2/nether_brick_nether_brick", "S2", [block(CENTER, "minecraft:nether_brick_fence"), block(SIDES["east"], "minecraft:nether_brick_fence")])
add("S3/oak_birch", "S3", [block(CENTER, "minecraft:oak_fence"), block(SIDES["east"], "minecraft:birch_fence")])
add("S3/bamboo_crimson", "S3", [block(CENTER, "minecraft:bamboo_fence"), block(SIDES["east"], "minecraft:crimson_fence")])

# S4 — çit ve kapak
for direction in range(4):
    for open_bit in (False, True):
        add(f"S4/trapdoor/d{direction}/{'open' if open_bit else 'closed'}", "S4", [
            block(CENTER, "minecraft:oak_fence"),
            block(SIDES["east"], "minecraft:trapdoor", direction=direction, open_bit=open_bit, upside_down_bit=False),
        ])

# S5 / S6 — "dolu komşu" tanımı ve istisnalar; çit ve cam panel için ayrı ayrı
NEIGHBOURS = [
    ("stone", "minecraft:stone", {}), ("glass", "minecraft:glass", {}), ("tinted_glass", "minecraft:tinted_glass", {}),
    ("oak_leaves", "minecraft:oak_leaves", {"persistent_bit": True}), ("slab_bottom", "minecraft:smooth_stone_slab", {"minecraft:vertical_half": "bottom"}),
    ("slab_top", "minecraft:smooth_stone_slab", {"minecraft:vertical_half": "top"}), ("double_slab", "minecraft:smooth_stone_double_slab", {}),
    ("chest", "minecraft:chest", {"minecraft:cardinal_direction": "north"}), ("hopper", "minecraft:hopper", {}), ("glowstone", "minecraft:glowstone", {}),
    ("ice", "minecraft:ice", {}), ("slime", "minecraft:slime", {}), ("honey_block", "minecraft:honey_block", {}), ("barrier", "minecraft:barrier", {}),
    ("pumpkin", "minecraft:pumpkin", {}), ("carved_pumpkin", "minecraft:carved_pumpkin", {}), ("lit_pumpkin", "minecraft:lit_pumpkin", {}),
    ("melon_block", "minecraft:melon_block", {}), ("white_shulker_box", "minecraft:white_shulker_box", {}), ("snow_layer_full", "minecraft:snow_layer", {"height": 7}),
    ("crafting_table", "minecraft:crafting_table", {}), ("oak_log", "minecraft:oak_log", {}), ("soul_sand", "minecraft:soul_sand", {}), ("mud", "minecraft:mud", {}),
    ("grass_path", "minecraft:grass_path", {}), ("farmland", "minecraft:farmland", {}), ("cactus", "minecraft:cactus", {}),
    ("enchanting_table", "minecraft:enchanting_table", {}), ("beacon", "minecraft:beacon", {}), ("scaffolding", "minecraft:scaffolding", {}),
    ("anvil", "minecraft:anvil", {}), ("bell", "minecraft:bell", {}), ("lantern", "minecraft:lantern", {}), ("tnt", "minecraft:tnt", {}),
    ("redstone_block", "minecraft:redstone_block", {}), ("sea_lantern", "minecraft:sea_lantern", {}), ("cobblestone_wall", "minecraft:cobblestone_wall", {}),
    ("iron_bars", "minecraft:iron_bars", {}), ("glass_pane", "minecraft:glass_pane", {}), ("oak_fence", "minecraft:oak_fence", {}),
]
for weirdo in range(4):
    NEIGHBOURS.append((f"oak_stairs_w{weirdo}", "minecraft:oak_stairs", {"weirdo_direction": weirdo, "upside_down_bit": False}))
for center_key, center_name in (("fence", "minecraft:oak_fence"), ("pane", "minecraft:glass_pane")):
    for key, name, states in NEIGHBOURS:
        question = "S7" if center_key == "pane" and key.endswith("_wall") else "S5"
        neighbour = {"at": SIDES["east"], "name": name}
        if states:
            neighbour["states"] = states
        add(f"S5/{center_key}/{key}", question, [block(CENTER, center_name), neighbour], read=[CENTER])

# S7 — cam panel ve duvar türleri
for wall in ("minecraft:blackstone_wall", "minecraft:mud_brick_wall", "minecraft:tuff_wall"):
    add(f"S7/pane_wall/{wall.split(':')[1]}", "S7", [block(CENTER, "minecraft:glass_pane"), block(SIDES["east"], wall)], read=[CENTER])

# S8 — parmaklık ve panel türleri birbirine
for left, right in (("copper_bars", "iron_bars"), ("glass_pane", "copper_bars"), ("hard_glass_pane", "glass_pane"),
                    ("white_stained_glass_pane", "glass_pane"), ("waxed_oxidized_copper_bars", "copper_bars")):
    add(f"S8/{left}/{right}", "S8", [block(CENTER, f"minecraft:{left}"), block(SIDES["east"], f"minecraft:{right}")])

# S9 — tuzak ipi
add("S9/trip_wire_line", "S9", [block([-1, 0, 0], "minecraft:trip_wire"), block(CENTER, "minecraft:trip_wire"), block(SIDES["east"], "minecraft:trip_wire")])
add("S9/trip_wire_stone", "S9", [block(CENTER, "minecraft:trip_wire"), block(SIDES["east"], "minecraft:stone")], read=[CENTER])
for direction in range(4):
    add(f"S9/trip_wire_hook/d{direction}", "S9", [
        block([2, 0, 0], "minecraft:stone"),
        block(SIDES["east"], "minecraft:tripwire_hook", direction=direction),
        block(CENTER, "minecraft:trip_wire"),
    ], read=[CENTER, SIDES["east"]])

# 6.2 / S10 — merdiven köşeleri: her yön ve yarı için dört komşu konumu × dört komşu yönü; yarı uyuşmazlığı ayrıca
for weirdo in range(4):
    for upside in (False, True):
        center = block(CENTER, "minecraft:oak_stairs", weirdo_direction=weirdo, upside_down_bit=upside)
        for side, offset in SIDES.items():
            for neighbour_weirdo in range(4):
                add(f"STAIRS/w{weirdo}/{'top' if upside else 'bottom'}/{side}/w{neighbour_weirdo}", "S10", [
                    center, block(offset, "minecraft:oak_stairs", weirdo_direction=neighbour_weirdo, upside_down_bit=upside),
                ], read=[CENTER])
            add(f"STAIRS/w{weirdo}/{'top' if upside else 'bottom'}/{side}/half_mismatch", "S10", [
                center, block(offset, "minecraft:oak_stairs", weirdo_direction=(weirdo + 2) % 4, upside_down_bit=not upside),
            ], read=[CENTER])

# S10 — Java'nın şekilAlabilir engeli: arka komşu dikken yanda aynı yönlü merdiven
for weirdo in range(4):
    for side, offset in SIDES.items():
        for back_side, back_offset in SIDES.items():
            if back_offset == offset:
                continue
            for back_weirdo in range(4):
                add(f"S10/can_take_shape/w{weirdo}/side_{side}/back_{back_side}_w{back_weirdo}", "S10", [
                    block(CENTER, "minecraft:oak_stairs", weirdo_direction=weirdo, upside_down_bit=False),
                    block(back_offset, "minecraft:oak_stairs", weirdo_direction=back_weirdo, upside_down_bit=False),
                    block(offset, "minecraft:oak_stairs", weirdo_direction=weirdo, upside_down_bit=False),
                ], read=[CENTER])

# S11 — güncelleme yayılımı ve dikey etki
add("S11/fence_neighbour_removed", "S11", [block(CENTER, "minecraft:oak_fence"), block(SIDES["east"], "minecraft:oak_fence")],
    read=[CENTER], then=[block(SIDES["east"], "minecraft:air")])
add("S11/pane_neighbour_replaced_by_stone", "S11", [block(CENTER, "minecraft:glass_pane"), block(SIDES["east"], "minecraft:glass_pane")],
    read=[CENTER], then=[block(SIDES["east"], "minecraft:stone")])
add("S11/fence_stone_above", "S11", [block(CENTER, "minecraft:oak_fence"), block([0, 1, 0], "minecraft:stone")], read=[CENTER])
add("S11/stairs_neighbour_removed", "S11", [
    block(CENTER, "minecraft:oak_stairs", weirdo_direction=3, upside_down_bit=False),
    block(SIDES["north"], "minecraft:oak_stairs", weirdo_direction=0, upside_down_bit=False),
], read=[CENTER], then=[block(SIDES["north"], "minecraft:air")])

ids = [s["id"] for s in scenarios]
assert len(ids) == len(set(ids)), "senaryo kimlikleri tekil olmalı"
out = Path(__file__).with_name("scenarios.json")
out.write_text(json.dumps({"formatVersion": 1, "scenarios": scenarios}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{len(scenarios)} senaryo yazıldı: {out}")
