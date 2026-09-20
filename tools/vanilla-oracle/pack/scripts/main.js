// Vanilla kahin: run_oracle.py'nin ürettiği scenarios.js'teki her senaryoyu kendi hücresine kurar,
// BDS'in hesapladığı blok durumlarını okur ve "ORACLE <etiket> <json>" satırları olarak konsola yazar.
// MODE "dump" ise kayıt dökümü ve yakıt ölçümü (dump.js), "faces" bağlantı yüzü tablosu (faces.js),
// "palette" ise kanonik blok durumu listesi (palette.js) çalışır.
import { world, system, BlockPermutation } from "@minecraft/server";
import { MODE, SCENARIOS, AREA, FUEL_GRID, FUEL_CAP_TICKS, FACE_BATCHES, PALETTE_PLAN } from "./scenarios.js";
import { dumpRegistries, measureFuel } from "./dump.js";
import { dumpPalette } from "./palette.js";
import { measureFaces } from "./faces.js";

const PLACE_TO_THEN_TICKS = 10;
const THEN_TO_READ_TICKS = 20;
const LOAD_TIMEOUT_TICKS = 2400;

const emit = (tag, data) => console.warn(`ORACLE ${tag} ${JSON.stringify(data)}`);
const at = (origin, offset) => ({ x: origin[0] + offset[0], y: origin[1] + offset[1], z: origin[2] + offset[2] });

function tryGetBlock(dimension, location) {
  try {
    return dimension.getBlock(location);
  } catch {
    return undefined;
  }
}

function apply(dimension, scenario, placements, errors) {
  for (const placement of placements ?? []) {
    try {
      const block = tryGetBlock(dimension, at(scenario.origin, placement.at));
      if (!block) {
        errors.push(`yuklenmemis konum ${placement.at}`);
        continue;
      }
      block.setPermutation(BlockPermutation.resolve(placement.name, placement.states ?? {}));
    } catch (error) {
      errors.push(`${placement.name} ${JSON.stringify(placement.states ?? {})} @${placement.at}: ${error}`);
    }
  }
}

function snapshot(dimension, scenario) {
  const cells = {};
  for (const offset of scenario.read) {
    const block = tryGetBlock(dimension, at(scenario.origin, offset));
    cells[offset.join(",")] = block ? { name: block.typeId, states: block.permutation.getAllStates() } : null;
  }
  return cells;
}

world.afterEvents.worldLoad.subscribe(() => {
  const dimension = world.getDimension("overworld");
  dimension.runCommand(`tickingarea add ${AREA.from.join(" ")} ${AREA.to.join(" ")} gears_vanilla_oracle true`);
  const corners = [
    { x: AREA.from[0], y: AREA.from[1], z: AREA.from[2] },
    { x: AREA.to[0], y: AREA.from[1], z: AREA.from[2] },
    { x: AREA.from[0], y: AREA.from[1], z: AREA.to[2] },
    { x: AREA.to[0], y: AREA.from[1], z: AREA.to[2] },
  ];
  const started = system.currentTick;
  const waiter = system.runInterval(() => {
    if (!corners.every((corner) => tryGetBlock(dimension, corner) !== undefined)) {
      if (system.currentTick - started > LOAD_TIMEOUT_TICKS) {
        system.clearRun(waiter);
        emit("FATAL", { reason: "senaryo alani yuklenmedi" });
        emit("DONE", { count: 0 });
      }
      return;
    }
    system.clearRun(waiter);

    if (MODE === "faces") {
      measureFaces(dimension, AREA, FACE_BATCHES, emit, () => emit("DONE", {}));
      return;
    }

    if (MODE === "palette") {
      dumpPalette(emit, () => emit("DONE", {}), PALETTE_PLAN);
      return;
    }

    if (MODE === "dump") {
      dumpRegistries(emit);
      measureFuel(dimension, FUEL_GRID, FUEL_CAP_TICKS, emit, () => emit("DONE", {}));
      return;
    }

    const errors = new Map(SCENARIOS.map((scenario) => [scenario.id, []]));
    for (const scenario of SCENARIOS) {
      apply(dimension, scenario, scenario.place, errors.get(scenario.id));
    }
    system.runTimeout(() => {
      for (const scenario of SCENARIOS) {
        apply(dimension, scenario, scenario.then, errors.get(scenario.id));
      }
      system.runTimeout(() => {
        for (const scenario of SCENARIOS) {
          emit("RESULT", { id: scenario.id, cells: snapshot(dimension, scenario), errors: errors.get(scenario.id) });
        }
        emit("DONE", { count: SCENARIOS.length });
      }, THEN_TO_READ_TICKS);
    }, PLACE_TO_THEN_TICKS);
  }, 10);
});
