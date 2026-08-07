# Staged 1.26.40 (protocol 2168) data — INCOMPLETE, NOT WIRED IN

This folder holds the parts of the 1.26.40 data set that are publicly available and
verified. **Nothing here is read by the build or the server.** It exists so the
remaining work does not have to start from scratch.

`ProtocolInfo.FEATURE_VERSION` is still `Bedrock_v1001` (1.26.30) and the shipped
`block_states.json` / `block_types.json` are unchanged.

## What changed in 1.26.40

23 new block types (586 block states) and 43 new items — the Poplar wood set,
`minecraft:straw_bed`, and 16 Cushions. All of it is **Drop 3 experimental**
content, gated behind a world toggle. No block was removed and no existing block
changed its state set, which was verified state by state.

Because of that, `BLOCK_STATE_VERSION`, `BLOCK_STATE_UPDATER` and
`ITEM_STATE_UPDATER` do not need to move: the block version is `1.21.60.33` on
both sides. Existing island worlds are also unaffected — Allay derives block
runtime IDs from a hash of name + states (`HashUtils.computeBlockStateHash`), not
from the palette index, so appending block types cannot shift existing IDs.

## Files here

| File | Source | Notes |
|---|---|---|
| `block_palette.nbt` | CloudburstMC/Data `v2168` | Converted to Allay's shape: `block_id`, `name_hash` and `network_id` stripped, only `name`/`states`/`version` kept. Verified: 17499 states / 1379 types, and the old 1.26.30 palette is a complete subset. |
| `item_components.nbt` | CloudburstMC/Data `v2168` | As published. |
| `creative_items.json` | CloudburstMC/Data `v2168` | JSON; the shipped file is `creative_items.nbt`, so this still needs converting. |
| `runtime_item_states.json` | CloudburstMC/Data `v2168` | Input for rebuilding `items_raw.json`. |
| `stripped_biome_definitions.json` | CloudburstMC/Data | **Not** refreshed by the `v2168` commit. Biomes rarely change, but treat as stale. |
| `mojang-blocks.json`, `mojang-items.json` | Mojang/bedrock-samples `1.26.40.5` | Official, stable. |

Language files are deliberately **not** staged here — 26 MB for no benefit, since they
are one command away and the Drop 3 item names are not in them anyway (those live in
the experimental pack, not in the base vanilla texts):

```bash
for f in data/resources/unpacked/lang_raw/vanilla/*.lang; do
    curl -sfL -o "$f" \
      "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/texts/$(basename "$f")"
done
```

## What is still missing

The blocker is **physical block properties** for the 23 new block types: hardness,
friction, collision shape, explosion resistance, burn odds and so on. That is what
`unpacked/block_states_raw.json` carries, and the runtime reads its derived output
(`block_states.json`), so new blocks cannot be registered without it.

Nobody publishes it for 1.26.40. CloudburstMC/Data has the equivalent file
(`block_properties.json`) but the `v2168` commit did not refresh it — it is still
at 16913 entries. Mojang's `mojang-blocks.json` gives identifiers and state
property names only, no physics.

Also missing: `block_types.json`, `block_tags.json`, `item_tags.json`,
`creative_groups.json`.

## How to finish it

The documented path (`docs/development/update-to-the-next-protocol-version.md`) is
Endstone DevTools, which dumps all ten files straight out of BDS. DevTools is
**Windows-only**: `src/endstone/core/CMakeLists.txt` defines
`ENDSTONE_DISABLE_DEVTOOLS` under `if (UNIX)`, and the tool is built on
ImGui + GLFW + OpenGL.

Two ways forward:

1. Run Endstone on a Windows machine and use the `devtools` console command. This
   produces every missing file and is by far the cheapest option.
2. Fork Endstone and build it on Linux with the GUI stripped. The dumping code is
   separable — `vanilla_data.cpp` gathers the data from BDS registries and
   `exportAll()` writes the files with plain `ofstream`, no GUI involved. It needs
   Clang 18+ with libc++ and a full Conan dependency tree.

Deriving the missing physics from analogous vanilla blocks (poplar planks behave
like oak planks) is possible but produces guessed rather than extracted data, and a
wrong hardness value shows up in game without any log output.
