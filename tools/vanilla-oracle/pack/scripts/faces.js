// Bağlantı yüzü tablosu: her blok durumunu hücrenin ortasına koyar, dört yanına sonda (çit ya da parmaklık) yerleştirir
// ve sondanın ortadaki bloğa bağlanıp bağlanmadığını okur. Satırlar "ORACLE FACE <json>" olarak yazılır.
// Bütün partiler main.js'in açtığı tek tickingarea'yı kullanır; partiler arasında alan fill ile havaya çevrilir.
// (Her parti için ayrı uzak alan açmak BDS 1.26.51.1'de üçüncü alanda takıldı: alan hiç yüklenmedi.)
import { system, BlockPermutation } from "@minecraft/server";

const CLEAR_TO_PLACE_TICKS = 2;
const SETTLE_TICKS = 6;
// Temizlenen katmanlar: hücre katmanı ve bir üstü (üstüne blok bırakan durumlar için).
const CLEAR_LAYERS = 2;
// Kuzey, doğu, güney, batı: sonda konumu ve sondanın ortaya bakan bağlantı durumu.
const SIDES = [
  { offset: [0, 0, -1], state: "minecraft:connection_south" },
  { offset: [1, 0, 0], state: "minecraft:connection_west" },
  { offset: [0, 0, 1], state: "minecraft:connection_north" },
  { offset: [-1, 0, 0], state: "minecraft:connection_east" },
];

function tryGetBlock(dimension, location) {
  try {
    return dimension.getBlock(location);
  } catch {
    return undefined;
  }
}

function clearArea(dimension, area) {
  // Tek fill en fazla 32768 blok; 160x160 katman tek komuta sığar.
  for (let layer = 0; layer < CLEAR_LAYERS; layer++) {
    const y = area.from[1] + layer;
    // Boş katmanda fill "0 blok değişti" der; sonucu hata sayılmaz.
    dimension.runCommand(`fill ${area.from[0]} ${y} ${area.from[2]} ${area.to[0]} ${y} ${area.to[2]} air`);
  }
  dimension.runCommand("kill @e[type=item]");
}

function runBatch(dimension, area, batch, emit, onDone) {
  clearArea(dimension, area);
  system.runTimeout(() => {
    const probe = BlockPermutation.resolve(batch.probe);
    for (const cell of batch.cells) {
      for (const side of probedSides(cell)) {
        const block = tryGetBlock(dimension, { x: cell.at[0] + side.offset[0], y: cell.at[1], z: cell.at[2] + side.offset[2] });
        if (!block) {
          emit("FATAL", { reason: `parti ${batch.index} yuklenmemis konum`, at: cell.at });
          onDone(false);
          return;
        }
        block.setPermutation(probe);
      }
    }
    for (const cell of batch.cells) {
      try {
        for (const extra of cell.context ?? []) {
          const at = { x: cell.at[0] + extra.offset[0], y: cell.at[1] + extra.offset[1], z: cell.at[2] + extra.offset[2] };
          tryGetBlock(dimension, at).setPermutation(BlockPermutation.resolve(extra.name, extra.states));
        }
        const block = tryGetBlock(dimension, { x: cell.at[0], y: cell.at[1], z: cell.at[2] });
        block.setPermutation(BlockPermutation.resolve(cell.name, cell.states));
      } catch (error) {
        cell.error = String(error);
      }
    }

    system.runTimeout(() => {
      for (const cell of batch.cells) {
        const center = tryGetBlock(dimension, { x: cell.at[0], y: cell.at[1], z: cell.at[2] });
        const probed = probedSides(cell);
        const faces = SIDES.map((side) => {
          if (!probed.includes(side)) {
            return null;
          }
          const block = tryGetBlock(dimension, { x: cell.at[0] + side.offset[0], y: cell.at[1], z: cell.at[2] + side.offset[2] });
          return block?.typeId === batch.probe ? block.permutation.getState(side.state) === true : null;
        });
        const stable = center?.typeId === cell.name && sameStates(center.permutation.getAllStates(), cell.states);
        emit("FACE", {
          i: cell.i,
          probe: batch.probe,
          faces,
          stable,
          ...(stable ? {} : { final: center ? { name: center.typeId, states: center.permutation.getAllStates() } : null }),
          ...(cell.error ? { error: cell.error } : {}),
          ...(cell.context ? { context: true } : {}),
        });
      }
      emit("BATCH", { index: batch.index, cells: batch.cells.length });
      onDone(true);
    }, SETTLE_TICKS);
  }, CLEAR_TO_PLACE_TICKS);
}

// Bağlamlı hücrede (köşeli merdiven) komşunun durduğu yan ölçülmez.
function probedSides(cell) {
  return cell.sides ? cell.sides.map((index) => SIDES[index]) : SIDES;
}

function sameStates(actual, expected) {
  return Object.keys(expected).every((key) => actual[key] === expected[key]);
}

export function measureFaces(dimension, area, batches, emit, onDone) {
  for (const rule of ["dofiretick false", "randomtickspeed 0", "tntexplodes false", "domobspawning false"]) {
    dimension.runCommand(`gamerule ${rule}`);
  }
  let next = 0;
  const step = () => {
    if (next >= batches.length) {
      clearArea(dimension, area);
      onDone();
      return;
    }
    const batch = batches[next++];
    runBatch(dimension, area, batch, emit, (ok) => (ok ? step() : onDone()));
  };
  step();
}
