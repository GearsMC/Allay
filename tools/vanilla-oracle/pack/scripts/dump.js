// Kayıt dökümü: Endstone dökümünün (yalnızca Windows) Linux BDS'te Script API ile alınabilen kısmı.
// Bloklarda varsayılan durum + etiketler, eşyalarda yığın sayısı + etiketler + bileşenler, ayrıca fırın
// deneyiyle yakıt süresi. Satırlar "ORACLE BLOCK|ITEM|FUEL <json>" olarak yazılır.
import { system, BlockTypes, BlockPermutation, ItemTypes, ItemStack } from "@minecraft/server";

// Fırın eşyayı yakarken lit_furnace olur. Allay verisindeki bütün yakıt süreleri 50'nin katı; iki tikte bir
// bakmak ölçümü 50'ye yuvarlanabilir kılar.
const FUEL_POLL_TICKS = 2;
const FUEL_SETUP_TICKS = 5;

export function dumpRegistries(emit) {
  for (const type of BlockTypes.getAll()) {
    try {
      const permutation = BlockPermutation.resolve(type.id);
      emit("BLOCK", {
        id: type.id,
        states: permutation.getAllStates(),
        tags: permutation.getTags().sort(),
        localizationKey: permutation.localizationKey,
      });
    } catch (error) {
      emit("BLOCK", { id: type.id, error: String(error) });
    }
  }

  for (const type of ItemTypes.getAll()) {
    try {
      const stack = new ItemStack(type, 1);
      const durability = stack.getComponent("minecraft:durability");
      const enchantable = stack.getComponent("minecraft:enchantable");
      const food = stack.getComponent("minecraft:food");
      emit("ITEM", {
        id: type.id,
        localizationKey: type.localizationKey,
        maxAmount: stack.maxAmount,
        tags: stack.getTags().sort(),
        maxDurability: durability ? durability.maxDurability : null,
        enchantSlots: enchantable ? [...enchantable.slots].sort() : null,
        food: food
          ? { nutrition: food.nutrition, saturationModifier: food.saturationModifier, canAlwaysEat: food.canAlwaysEat }
          : null,
      });
    } catch (error) {
      emit("ITEM", { id: type.id, error: String(error) });
    }
  }
}

// grid: { origin: [x, y, z], spacing, columns, capacity } — alanı run_oracle.py önceden yükler.
export function measureFuel(dimension, grid, capTicks, emit, onDone) {
  const ids = ItemTypes.getAll().map((type) => type.id).sort();
  if (ids.length > grid.capacity) {
    emit("FATAL", { reason: `yakit izgarasi ${grid.capacity} hucre, esya ${ids.length}` });
    onDone();
    return;
  }
  const cells = ids.map((id, index) => ({
    id,
    at: [
      grid.origin[0] + (index % grid.columns) * grid.spacing,
      grid.origin[1],
      grid.origin[2] + Math.floor(index / grid.columns) * grid.spacing,
    ],
  }));
  const at = (cell) => ({ x: cell.at[0], y: cell.at[1], z: cell.at[2] });
  for (const cell of cells) {
    try {
      dimension.getBlock(at(cell)).setPermutation(BlockPermutation.resolve("minecraft:furnace"));
    } catch (error) {
      cell.error = `firin kurulamadi: ${error}`;
    }
  }

  // Blok varlığı yerleştirmeden sonraki tikte hazır olur.
  system.runTimeout(() => {
    const pending = [];
    for (const cell of cells) {
      if (cell.error) {
        continue;
      }
      try {
        const container = dimension.getBlock(at(cell)).getComponent("minecraft:inventory").container;
        container.setItem(0, new ItemStack("minecraft:cobblestone", 64));
        container.setItem(1, new ItemStack(cell.id, 1));
        pending.push(cell);
      } catch (error) {
        cell.error = `esya konamadi: ${error}`;
      }
    }

    const started = system.currentTick;
    const poller = system.runInterval(() => {
      const now = system.currentTick;
      for (let i = pending.length - 1; i >= 0; i--) {
        const cell = pending[i];
        const typeId = dimension.getBlock(at(cell)).typeId;
        if (typeId === "minecraft:lit_furnace" && cell.litAt === undefined) {
          cell.litAt = now;
        } else if (typeId === "minecraft:furnace" && cell.litAt !== undefined) {
          cell.ticks = now - cell.litAt;
          pending.splice(i, 1);
        }
      }
      if (pending.length === 0 || now - started > capTicks) {
        system.clearRun(poller);
        for (const cell of cells) {
          emit("FUEL", {
            id: cell.id,
            ticks: cell.ticks ?? (cell.litAt === undefined ? 0 : null),
            capped: cell.ticks === undefined && cell.litAt !== undefined,
            ...(cell.error ? { error: cell.error } : {}),
          });
        }
        onDone();
      }
    }, FUEL_POLL_TICKS);
  }, FUEL_SETUP_TICKS);
}
