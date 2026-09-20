#!/usr/bin/env python3
"""Vanilla kahini çalıştırır: BDS'i indirir, senaryoları kurar, BDS'in hesapladığı blok durumlarını altın tabloya yazar.

Kullanım:
    python3 tools/vanilla-oracle/run_oracle.py                 # Mojang'ın yayınladığı son Linux BDS sürümü
    python3 tools/vanilla-oracle/run_oracle.py --bds-version 1.26.51.1
    python3 tools/vanilla-oracle/run_oracle.py --mode dump --output <dosya>   # kayıt dökümü + yakıt ölçümü
    python3 tools/vanilla-oracle/run_oracle.py --mode faces --output <dosya>  # çit/parmaklık bağlantı yüzü tablosu
    python3 tools/vanilla-oracle/run_oracle.py --mode palette --output <dosya> # kanonik blok durumu listesi

BDS depoya konmaz; önbellek dizinine indirilir (varsayılan ~/.cache/gears-vanilla-oracle).
"""
import argparse
import gzip
import io
import struct
import hashlib
import json
import math
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
import zipfile
from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parent
REPO_ROOT = TOOL_DIR.parent.parent
LINKS_API = "https://net-secondary.web.minecraft-services.net/api/v1.0/download/links"
LINUX_URL = "https://www.minecraft.net/bedrockdedicatedserver/bin-linux/bedrock-server-{version}.zip"
USER_AGENT = "Mozilla/5.0 (gears-vanilla-oracle)"
LEVEL_NAME = "gears_vanilla_oracle"
PACK_DIR_NAME = "gears_vanilla_oracle"
BASE_Y = -60  # düz dünyada çimin hemen üstü
CELL_SIZE = 5  # hücre içi uzaklık en fazla 2; komşu hücrelerin blokları birbirine değmez
MAX_TICKING_CHUNKS = 100
# Yakıt ölçümü: fırınlar iki blok arayla dizilir. 48x48 = 2304 hücre; 26.50'de 2076 eşya var.
FUEL_SPACING = 2
FUEL_COLUMNS = 48
FUEL_CAP_TICKS = 2500  # en uzun ölçülen süre; üstü "capped" (Allay'de 2400 üstü 4 eşya var)


def http_get(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def latest_linux_version():
    links = json.loads(http_get(LINKS_API))["result"]["links"]
    url = next(link["downloadUrl"] for link in links if link["downloadType"] == "serverBedrockLinux")
    return url.rsplit("bedrock-server-", 1)[1].removesuffix(".zip")


def ensure_bds(version, cache_dir):
    server_dir = cache_dir / f"bedrock-server-{version}"
    binary = server_dir / "bedrock_server"
    if binary.exists():
        return server_dir
    cache_dir.mkdir(parents=True, exist_ok=True)
    archive = cache_dir / f"bedrock-server-{version}.zip"
    if not archive.exists():
        print(f"BDS {version} indiriliyor...", flush=True)
        archive.write_bytes(http_get(LINUX_URL.format(version=version)))
    with zipfile.ZipFile(archive) as zf:
        zf.extractall(server_dir)
    binary.chmod(0o755)
    return server_dir


def layout(scenarios):
    columns = math.ceil(math.sqrt(len(scenarios)))
    placed = []
    for index, scenario in enumerate(scenarios):
        column, row = index % columns, index // columns
        placed.append({**scenario, "origin": [column * CELL_SIZE, BASE_Y, row * CELL_SIZE]})
    rows = math.ceil(len(scenarios) / columns)
    area = {"from": [-CELL_SIZE, BASE_Y, -CELL_SIZE], "to": [columns * CELL_SIZE, BASE_Y, rows * CELL_SIZE]}
    chunks_x = area["to"][0] // 16 - area["from"][0] // 16 + 1
    chunks_z = area["to"][2] // 16 - area["from"][2] // 16 + 1
    if chunks_x * chunks_z > MAX_TICKING_CHUNKS:
        sys.exit(f"senaryo alanı {chunks_x * chunks_z} chunk; tickingarea sınırı {MAX_TICKING_CHUNKS}")
    return placed, area


def fuel_layout():
    grid = {"origin": [0, BASE_Y, 0], "spacing": FUEL_SPACING, "columns": FUEL_COLUMNS, "capacity": FUEL_COLUMNS * FUEL_COLUMNS}
    span = FUEL_COLUMNS * FUEL_SPACING
    area = {"from": [-FUEL_SPACING, BASE_Y, -FUEL_SPACING], "to": [span, BASE_Y, span]}
    return grid, area


# Bağlantı yüzü tablosu: sonda türleri (yanmaz, ateş komşuyu yakmasın) ve hücre düzeni.
FACE_PROBES = ["minecraft:nether_brick_fence", "minecraft:iron_bars"]
FACE_SPACING = 3  # ortadaki blok + dört yanında sonda; komşu hücrenin sondası ortadaki bloğa değmez
FACE_COLUMNS = 52  # 2 + 51 x 3 + 1 = 156 < 160: parti alanı chunk sınırına hizalı 10x10 chunk içinde kalır
FACE_LIQUID_SPACING = 12  # su/lav akıp komşu hücreyi bozmasın
FACE_AREA_BLOCKS = 160  # tickingarea en fazla 100 chunk; alan 0..159 (tam 10 chunk)
LIQUIDS = ("minecraft:water", "minecraft:flowing_water", "minecraft:lava", "minecraft:flowing_lava")
# Köşeli merdiven tek başına kararlı değil: BDS köşeyi komşulara göre yeniden hesaplar. Bu durumlar köşeyi oluşturan
# komşu merdivenle birlikte kurulur ve yalnızca boş kalan üç yan ölçülür (komşunun yanı bu durumda zaten doludur).
FACE_CONTEXT_SPACING = 4  # merkez + komşu/sonda halkası; komşu hücrelerin halkaları arasında bir boş blok
FACE_CONTEXT_COLUMNS = 39  # 2 + 38 x 4 + 1 = 155 < 160
FACE_SIDE_OFFSETS = {"north": [0, 0, -1], "east": [1, 0, 0], "south": [0, 0, 1], "west": [-1, 0, 0]}
FACE_SIDE_ORDER = ["north", "east", "south", "west"]  # faces.js SIDES sırası


def read_block_palette(path):
    """data/resources/unpacked/block_palette.nbt (gzip, büyük uçlu NBT) -> [(ad, {durum: (nbt tipi, değer)})]."""
    f = io.BytesIO(gzip.decompress(path.read_bytes()))

    def read(fmt):
        size = struct.calcsize(">" + fmt)
        return struct.unpack(">" + fmt, f.read(size))[0]

    def payload(tag):
        if tag in (1, 2, 3, 4, 5, 6):
            return read({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[tag])
        if tag == 8:
            return f.read(read("H")).decode("utf-8")
        if tag == 9:
            element, count = read("B"), read("i")
            return [payload(element) for _ in range(count)]
        if tag == 10:
            result = {}
            while True:
                child = read("B")
                if child == 0:
                    return result
                key = f.read(read("H")).decode("utf-8")
                result[key] = (child, payload(child))
        raise ValueError(f"desteklenmeyen NBT tipi {tag}")

    root_tag = read("B")
    f.read(read("H"))
    root = payload(root_tag)
    return [(entry["name"][1], entry["states"][1]) for entry in root["blocks"][1]]


def stair_corner_contexts():
    """Altın tablodaki STAIRS senaryolarından (yön, üst yarı, köşe) → (komşu yanı, komşu yönü) eşlemesi."""
    scenarios = json.loads((TOOL_DIR / "scenarios.json").read_text(encoding="utf-8"))["scenarios"]
    golden_files = sorted((REPO_ROOT / "server" / "src" / "test" / "resources" / "vanilla-oracle").glob("*.json"))
    golden = json.loads(golden_files[-1].read_text(encoding="utf-8"))["results"]
    contexts = {}
    for scenario in scenarios:
        if not scenario["id"].startswith("STAIRS/"):
            continue
        cell = golden[scenario["id"]]["cells"]["0,0,0"]["states"]
        if cell["minecraft:corner"] == "none":
            continue
        neighbor = scenario["place"][1]
        side = next(name for name, offset in FACE_SIDE_OFFSETS.items() if offset == neighbor["at"])
        key = (cell["weirdo_direction"], bool(cell["upside_down_bit"]), cell["minecraft:corner"])
        contexts[key] = (side, neighbor["states"]["weirdo_direction"])
    if len(contexts) != 32:
        sys.exit(f"altın tabloda {len(contexts)} köşe bağlamı var, 32 bekleniyordu")
    return contexts


def face_script_states(palette, dump_blocks):
    """Palet durumlarını Script API tiplerine çevirir (palet sırası korunur)."""
    cells = []
    for index, (name, states) in enumerate(palette):
        defaults = (dump_blocks.get(name) or {}).get("states", {})
        js_states = {}
        for key, (tag, value) in states.items():
            # Script API boolean durumu true/false ister; palet bayt tutar. Tip dökümdeki varsayılandan okunur.
            js_states[key] = bool(value) if isinstance(defaults.get(key), bool) else value
        cells.append({"i": index, "name": name, "states": js_states})
    return cells


def state_key(name, states):
    return name + json.dumps(sorted(states.items()))


def face_batches(palette, dump_blocks):
    """Palet durumlarını sonda başına partilere böler."""
    cells = face_script_states(palette, dump_blocks)

    contexts = stair_corner_contexts()
    corner_cells = []
    for cell in cells:
        corner = cell["states"].get("minecraft:corner")
        if corner is None or corner == "none":
            continue
        side, neighbor_direction = contexts[(cell["states"]["weirdo_direction"], cell["states"]["upside_down_bit"], corner)]
        corner_cells.append({
            **cell,
            "context": [{"offset": FACE_SIDE_OFFSETS[side], "name": cell["name"], "states": {
                "weirdo_direction": neighbor_direction, "upside_down_bit": cell["states"]["upside_down_bit"]}}],
            "sides": [index for index, name in enumerate(FACE_SIDE_ORDER) if name != side],
        })

    batches = []
    for probe in FACE_PROBES:
        normal = [cell for cell in cells if cell["name"] not in LIQUIDS]
        liquid = [cell for cell in cells if cell["name"] in LIQUIDS]
        per_batch = FACE_COLUMNS * FACE_COLUMNS
        groups = [(normal[i:i + per_batch], FACE_SPACING, FACE_COLUMNS) for i in range(0, len(normal), per_batch)]
        groups.append((liquid, FACE_LIQUID_SPACING, 12))
        per_context = FACE_CONTEXT_COLUMNS * FACE_CONTEXT_COLUMNS
        groups += [(corner_cells[i:i + per_context], FACE_CONTEXT_SPACING, FACE_CONTEXT_COLUMNS)
                   for i in range(0, len(corner_cells), per_context)]
        for group, spacing, columns in groups:
            index = len(batches)
            # Bütün partiler aynı alanı kullanır: BDS 1.26.51.1 üçüncü uzak tickingarea'yı hiç yüklemedi
            # (iki alan açılıp kaldırıldıktan sonra); alan bir kez açılır, partiler arasında fill ile temizlenir.
            placed = [{**cell, "at": [2 + (n % columns) * spacing, BASE_Y, 2 + (n // columns) * spacing]}
                      for n, cell in enumerate(group)]
            last = max(max(c["at"][0], c["at"][2]) for c in placed) + 1
            if last >= FACE_AREA_BLOCKS:
                sys.exit(f"parti {index} alanı {last} blok, sınır {FACE_AREA_BLOCKS}")
            batches.append({"index": index, "probe": probe, "cells": placed})
    return batches


def install(server_dir, scenarios, area, port, mode="scenarios", fuel_grid=None, face_batches_data=None,
            palette_plan=None):
    pack_target = server_dir / "development_behavior_packs" / PACK_DIR_NAME
    shutil.rmtree(pack_target, ignore_errors=True)
    shutil.copytree(TOOL_DIR / "pack", pack_target)
    (pack_target / "scripts" / "scenarios.js").write_text(
        "export const MODE = " + json.dumps(mode) + ";\n"
        + "export const SCENARIOS = " + json.dumps(scenarios, ensure_ascii=False) + ";\n"
        + "export const AREA = " + json.dumps(area) + ";\n"
        + "export const FUEL_GRID = " + json.dumps(fuel_grid) + ";\n"
        + f"export const FUEL_CAP_TICKS = {FUEL_CAP_TICKS};\n"
        + "export const FACE_BATCHES = " + json.dumps(face_batches_data or [], ensure_ascii=False) + ";\n"
        + "export const PALETTE_PLAN = " + json.dumps(palette_plan or [], ensure_ascii=False) + ";\n",
        encoding="utf-8",
    )

    manifest = json.loads((TOOL_DIR / "pack" / "manifest.json").read_text(encoding="utf-8"))
    world_dir = server_dir / "worlds" / LEVEL_NAME
    shutil.rmtree(world_dir, ignore_errors=True)
    world_dir.mkdir(parents=True)
    (world_dir / "world_behavior_packs.json").write_text(
        json.dumps([{"pack_id": manifest["header"]["uuid"], "version": manifest["header"]["version"]}]), encoding="utf-8")

    properties = server_dir / "server.properties"
    wanted = {
        "server-port": str(port), "server-portv6": str(port + 1), "level-name": LEVEL_NAME, "level-type": "FLAT",
        "gamemode": "creative", "allow-cheats": "true", "content-log-console-output-enabled": "true",
        "enable-lan-visibility": "false",
    }
    lines, seen = [], set()
    for line in properties.read_text(encoding="utf-8").splitlines():
        key = line.split("=", 1)[0]
        if key in wanted:
            lines.append(f"{key}={wanted[key]}")
            seen.add(key)
        else:
            lines.append(line)
    lines += [f"{key}={value}" for key, value in wanted.items() if key not in seen]
    properties.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return manifest


def run(server_dir, timeout):
    process = subprocess.Popen(
        ["./bedrock_server"], cwd=server_dir, env={**os.environ, "LD_LIBRARY_PATH": "."},
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    results, fatal, log = {}, [], []
    records = {"BLOCK": [], "ITEM": [], "FUEL": [], "FACE": [], "PALETTE": []}
    done = threading.Event()

    def reader():
        for raw in process.stdout:
            line = raw.rstrip("\n")
            log.append(line)
            marker = line.find("ORACLE ")
            if marker < 0:
                if "ERROR" in line or "[Scripting]" in line:
                    print(line, flush=True)
                continue
            tag, _, payload = line[marker + len("ORACLE "):].partition(" ")
            data = json.loads(payload) if payload else {}
            if tag == "RESULT":
                results[data["id"]] = data
            elif tag in records:
                records[tag].append(data)
            elif tag == "BATCH":
                print(f"parti {data['index']} bitti ({data['cells']} hücre)", flush=True)
            elif tag == "FATAL":
                fatal.append(data)
            elif tag == "DONE":
                done.set()

    threading.Thread(target=reader, daemon=True).start()
    deadline = time.time() + timeout
    while time.time() < deadline and not done.is_set() and process.poll() is None:
        done.wait(timeout=1)
    if process.poll() is None:
        process.stdin.write("stop\n")
        process.stdin.flush()
        try:
            process.wait(timeout=60)
        except subprocess.TimeoutExpired:
            process.kill()
    return results, records, fatal, done.is_set(), log


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bds-version", help="ör. 1.26.51.1; verilmezse Mojang'ın son Linux sürümü")
    parser.add_argument("--cache-dir", type=Path, default=Path.home() / ".cache" / "gears-vanilla-oracle")
    parser.add_argument("--port", type=int, default=19140, help="BDS UDP portu (Allay'in 19132'siyle çakışmamalı)")
    parser.add_argument("--timeout", type=int, default=600)
    parser.add_argument("--output", type=Path, help="altın tablo yolu")
    parser.add_argument("--mode", choices=["scenarios", "dump", "faces", "palette"], default="scenarios")
    args = parser.parse_args()

    version = args.bds_version or latest_linux_version()
    if args.mode == "dump":
        return dump(args, version)
    if args.mode == "faces":
        return faces(args, version)
    if args.mode == "palette":
        return palette_dump(args, version)
    scenario_file = TOOL_DIR / "scenarios.json"
    scenarios = json.loads(scenario_file.read_text(encoding="utf-8"))["scenarios"]
    server_dir = ensure_bds(version, args.cache_dir)
    placed, area = layout(scenarios)
    manifest = install(server_dir, placed, area, args.port)
    print(f"BDS {version}, {len(placed)} senaryo, alan {area}", flush=True)

    results, _, fatal, finished, log = run(server_dir, args.timeout)
    (args.cache_dir / "last_run.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    if fatal or not finished:
        sys.exit(f"kahin tamamlanmadı: bitti={finished}, ölümcül={fatal}; günlük: {args.cache_dir / 'last_run.log'}")

    by_id = {scenario["id"]: scenario for scenario in scenarios}
    missing = sorted(set(by_id) - set(results))
    errored = {sid: data["errors"] for sid, data in results.items() if data.get("errors")}
    table = {
        "bedrockVersion": version,
        "scriptModule": manifest["dependencies"][0],
        "scenarioSha1": hashlib.sha1(scenario_file.read_bytes()).hexdigest(),
        "results": {
            sid: {"question": by_id[sid]["question"], "cells": results[sid]["cells"], **({"errors": errored[sid]} if sid in errored else {})}
            for sid in sorted(results)
        },
    }
    output = args.output or REPO_ROOT / "server" / "src" / "test" / "resources" / "vanilla-oracle" / f"{version}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(table, ensure_ascii=False, indent=1, sort_keys=False) + "\n", encoding="utf-8")
    print(f"altın tablo yazıldı: {output} ({len(results)} sonuç)")
    if missing or errored:
        print(f"eksik sonuç: {len(missing)} {missing[:5]}")
        for sid, errors in list(errored.items())[:20]:
            print(f"  hata {sid}: {errors}")
        sys.exit(1)


def dump(args, version):
    server_dir = ensure_bds(version, args.cache_dir)
    grid, area = fuel_layout()
    manifest = install(server_dir, [], area, args.port, mode="dump", fuel_grid=grid)
    print(f"BDS {version}, kayıt dökümü + yakıt ölçümü, alan {area}", flush=True)

    _, records, fatal, finished, log = run(server_dir, args.timeout)
    (args.cache_dir / "last_run.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    if fatal or not finished:
        sys.exit(f"döküm tamamlanmadı: bitti={finished}, ölümcül={fatal}; günlük: {args.cache_dir / 'last_run.log'}")

    def by_id(rows):
        return {row["id"]: {k: v for k, v in row.items() if k != "id"} for row in sorted(rows, key=lambda r: r["id"])}

    table = {
        "bedrockVersion": version,
        "scriptModule": manifest["dependencies"][0],
        "fuelCapTicks": FUEL_CAP_TICKS,
        "blocks": by_id(records["BLOCK"]),
        "items": by_id(records["ITEM"]),
        "fuel": by_id(records["FUEL"]),
    }
    output = args.output or TOOL_DIR / f"registry-dump-{version}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(table, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    errors = {kind: [r["id"] for r in rows if r.get("error")] for kind, rows in records.items()}
    print(f"döküm yazıldı: {output} (blok {len(table['blocks'])}, eşya {len(table['items'])}, yakıt {len(table['fuel'])})")
    for kind, ids in errors.items():
        if ids:
            print(f"  {kind} hatası {len(ids)}: {ids[:10]}")


# Mojang'ın meta verisinde olmayan bloklar (oyunda 1477 tür, listede 1463). Betiğin kendi `getAllStates()`'i
# bunlarda eski takma adları da veriyor (kullanımdan kalkmış örsün `damage`/`direction`'ı, purpur'un `chisel_type`'ı)
# ve tahta yazının yön aralığını dar ölçüyor; dördü elle yazılır. Kullanımdan kalkmış bloklar olduğu için
# sürümle değişmiyorlar.
PALETTE_OVERRIDES = {
    "minecraft:chalkboard": {"properties": ["direction"], "values": {"direction": list(range(16))}},
    "minecraft:deprecated_anvil": {"properties": ["minecraft:cardinal_direction"]},
    "minecraft:deprecated_purpur_block_1": {"properties": ["pillar_axis"]},
    "minecraft:deprecated_purpur_block_2": {"properties": ["pillar_axis"]},
}


def palette_plan():
    """Blok başına özellik adları, Mojang'ın kendi meta verisinden (bedrock-samples).

    Betik API'sinin `getAllStates()`'i eski takma adları da veriyor (acacia_wood'da `wood_type`), palette onlar yok.
    Mojang'ın listesi 1463 blok kapsıyor; oyunda 1477 tür var (gizli/kullanımdan kalkmış bloklar), kalanlar için
    betik kendi ad listesini kullanır.
    """
    path = REPO_ROOT / "data" / "resources" / "unpacked" / "mojang-blocks.json"
    blocks = json.loads(path.read_text(encoding="utf-8"))["data_items"]
    plan = [{"id": block["name"], "properties": sorted(p["name"] for p in block.get("properties", []))}
            for block in blocks]
    plan += [{"id": block_id, **override} for block_id, override in PALETTE_OVERRIDES.items()]
    return plan


def palette_dump(args, version):
    server_dir = ensure_bds(version, args.cache_dir)
    # Palet için ölçüm alanı gerekmiyor; kayıt defteri dünyadan bağımsız okunuyor, küçük bir alan yeter.
    area = {"from": [0, BASE_Y, 0], "to": [15, BASE_Y, 15]}
    manifest = install(server_dir, [], area, args.port, mode="palette", palette_plan=palette_plan())
    print(f"BDS {version}, kanonik blok durumu listesi", flush=True)

    _, records, fatal, finished, log = run(server_dir, args.timeout)
    (args.cache_dir / "last_run.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    if fatal or not finished:
        sys.exit(f"palet dökümü tamamlanmadı: bitti={finished}, ölümcül={fatal}; günlük: {args.cache_dir / 'last_run.log'}")

    rows = sorted(records["PALETTE"], key=lambda row: row["id"])
    states = [{"name": row["id"], "states": state} for row in rows for state in row.get("states", [])]
    table = {
        "bedrockVersion": version,
        "scriptModule": manifest["dependencies"][0],
        "blockTypes": len(rows),
        "blockStates": len(states),
        "palette": states,
    }
    output = args.output or TOOL_DIR / f"palette-{version}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(table, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    errored = [row for row in rows if row.get("error")]
    rejected = sum(row.get("rejected", 0) for row in rows)
    print(f"palet yazıldı: {output} ({len(rows)} blok türü, {len(states)} durum, {rejected} kombinasyon reddedildi)")
    for row in errored[:20]:
        print(f"  hata {row['id']}: {row['error']}")
    if errored:
        print(f"  toplam hatalı tür: {len(errored)}")


def faces(args, version):
    palette_path = REPO_ROOT / "data" / "resources" / "unpacked" / "block_palette.nbt"
    palette_sha1 = hashlib.sha1(palette_path.read_bytes()).hexdigest()
    palette = read_block_palette(palette_path)
    dump_file = REPO_ROOT / "data" / "resources" / "unpacked" / "bds_registry_dump.json"
    dump_blocks = json.loads(dump_file.read_text(encoding="utf-8"))["blocks"]
    batches = face_batches(palette, dump_blocks)
    server_dir = ensure_bds(version, args.cache_dir)
    # Başlangıç alanı ölçüm alanının kendisidir: chunk sınırına hizalı 10x10 chunk.
    area = {"from": [0, BASE_Y, 0], "to": [FACE_AREA_BLOCKS - 1, BASE_Y, FACE_AREA_BLOCKS - 1]}
    manifest = install(server_dir, [], area, args.port, mode="faces", face_batches_data=batches)
    print(f"BDS {version}, bağlantı yüzü tablosu: {len(palette)} durum, {len(batches)} parti", flush=True)

    _, records, fatal, finished, log = run(server_dir, args.timeout)
    (args.cache_dir / "last_run.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    if fatal or not finished:
        sys.exit(f"ölçüm tamamlanmadı: bitti={finished}, ölümcül={fatal}; günlük: {args.cache_dir / 'last_run.log'}")

    by_probe = {probe: {} for probe in FACE_PROBES}
    for row in records["FACE"]:
        # Köşeli merdiven iki kez ölçülür; bağlamlı ölçüm (sonradan gelir) tek başına ölçümün yerini alır.
        if row["i"] in by_probe[row["probe"]] and not row.get("context"):
            continue
        by_probe[row["probe"]][row["i"]] = row
    # Ham tablo: palet sırasıyla satır başına bir durum. Hücre "KDGB" yüz dizisidir (1 bağlandı, 0 bağlanmadı,
    # - ölçülmedi); blok ölçüm sırasında başka bir duruma döndüyse [yüzler, son durumun palet sırası]. Yüzler o son
    # duruma aittir. Türetme kuralları Allay tarafında: data/.../ConnectionFaceImport.
    index_by_state, keys_by_name = {}, {}
    for cell_data in face_script_states(palette, dump_blocks):
        index_by_state[state_key(cell_data["name"], cell_data["states"])] = cell_data["i"]
        keys_by_name[cell_data["name"]] = set(cell_data["states"])

    def cell(row):
        faces = "".join("-" if face is None else "1" if face else "0" for face in row["faces"])
        if row["stable"]:
            return faces
        final = row.get("final")
        final_index = None
        if final:
            # Script API paletin dışında eski adlı durumlar da döndürüyor (wall_block_type, wood_type); atılır.
            known = keys_by_name.get(final["name"], set())
            final_index = index_by_state.get(
                state_key(final["name"], {key: value for key, value in final["states"].items() if key in known}))
        if final is not None and final_index is None:
            sys.exit(f"son durum palette yok: {final}")
        return [faces, final_index]

    lines = []
    for index, (name, _) in enumerate(palette):
        rows = [by_probe[probe].get(index) for probe in FACE_PROBES]
        if any(row is None for row in rows):
            sys.exit(f"ölçülmemiş durum: {index} {name}")
        if any(row.get("error") for row in rows):
            sys.exit(f"durum kurulamadı: {index} {name}: {[row.get('error') for row in rows]}")
        lines.append(json.dumps([name] + [cell(row) for row in rows], ensure_ascii=False, separators=(",", ":")))
    header = {
        "bedrockVersion": version, "scriptModule": manifest["dependencies"][0],
        # Satırlar palet SIRASINA göre numaralı; palet değişirse tablo sessizce kayar. Parmak izi bunu yakalar.
        "palette": "block_palette.nbt", "paletteSha1": palette_sha1, "probes": FACE_PROBES, "sides": FACE_SIDE_ORDER,
    }
    output = args.output or TOOL_DIR / f"connection-faces-{version}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    body = ",\n".join(lines)
    output.write_text(json.dumps(header, ensure_ascii=False)[:-1] + ',"states":[\n' + body + "\n]}\n", encoding="utf-8")
    unstable = sum(1 for probe in FACE_PROBES for row in by_probe[probe].values() if not row["stable"])
    print(f"tablo yazıldı: {output} ({len(lines)} durum, kararsız ölçüm {unstable})")


if __name__ == "__main__":
    main()
