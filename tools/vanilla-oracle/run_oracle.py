#!/usr/bin/env python3
"""Vanilla kahini çalıştırır: BDS'i indirir, senaryoları kurar, BDS'in hesapladığı blok durumlarını altın tabloya yazar.

Kullanım:
    python3 tools/vanilla-oracle/run_oracle.py                 # Mojang'ın yayınladığı son Linux BDS sürümü
    python3 tools/vanilla-oracle/run_oracle.py --bds-version 1.26.51.1
    python3 tools/vanilla-oracle/run_oracle.py --mode dump --output <dosya>   # kayıt dökümü + yakıt ölçümü

BDS depoya konmaz; önbellek dizinine indirilir (varsayılan ~/.cache/gears-vanilla-oracle).
"""
import argparse
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


def install(server_dir, scenarios, area, port, mode="scenarios", fuel_grid=None):
    pack_target = server_dir / "development_behavior_packs" / PACK_DIR_NAME
    shutil.rmtree(pack_target, ignore_errors=True)
    shutil.copytree(TOOL_DIR / "pack", pack_target)
    (pack_target / "scripts" / "scenarios.js").write_text(
        "export const MODE = " + json.dumps(mode) + ";\n"
        + "export const SCENARIOS = " + json.dumps(scenarios, ensure_ascii=False) + ";\n"
        + "export const AREA = " + json.dumps(area) + ";\n"
        + "export const FUEL_GRID = " + json.dumps(fuel_grid) + ";\n"
        + f"export const FUEL_CAP_TICKS = {FUEL_CAP_TICKS};\n",
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
    records = {"BLOCK": [], "ITEM": [], "FUEL": []}
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
    parser.add_argument("--mode", choices=["scenarios", "dump"], default="scenarios")
    args = parser.parse_args()

    version = args.bds_version or latest_linux_version()
    if args.mode == "dump":
        return dump(args, version)
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


if __name__ == "__main__":
    main()
