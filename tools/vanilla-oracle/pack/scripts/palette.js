// Kanonik blok durumu listesi (palet): blok türü başına özelliklerin çarpımı.
// Palet ağda gelmiyor — istemci kendi içinde taşıyor — bu yüzden tek kaynağı oyunun kendisi. Amaç
// CloudburstMC/Data'nın block_palette.nbt'sinin yerini almak.
//
// İki bilgi birleştirilir:
//  1. Özellik ADLARI: run_oracle.py'nin Mojang'ın kendi meta verisinden (mojang-blocks.json) kurduğu plan.
//     Betik API'sinin `getAllStates()`'i eski takma adları da veriyor (acacia_wood'da `wood_type`, allium'da
//     `flower_type`); onlar palette yok. Planda olmayan blok için (Mojang listesi 1463, oyunda 1477 tür)
//     yedek olarak `getAllStates()` kullanılır.
//  2. Özellik DEĞERLERİ: blok başına ölçülür. Genel liste `age` için 0-15 der ama kakao yalnızca 0-2 kullanır;
//     `BlockPermutation.resolve` aralık dışı değeri sessizce varsayılana düşürdüğü için geri okuma testi
//     (istenen === okunan) o bloğun gerçek değer kümesini verir.
//
// Satırlar "ORACLE PALETTE <json>" olarak yazılır: blok türü başına bir satır.
import { system, BlockTypes, BlockPermutation, BlockStates } from "@minecraft/server";

// Tek bir blok türünün çarpımı bunu aşarsa durum listesi güvenilmez demektir; ölçüm yerine hata yazılır.
const MAX_COMBINATIONS = 8192;
// İş, bu kadar çözümlemeden sonra sıraya bırakılır; yoksa betik gözcüsü uzun döngüyü keser.
const RESOLVES_PER_YIELD = 256;

// validValues dizi gibi davranır ama yinelenebilir değildir: yayma (...) "value is not iterable" atar.
function readArray(value) {
  const out = [];
  for (let i = 0; i < value.length; i++) {
    out.push(value[i]);
  }
  return out;
}

function globalValues(name, cache) {
  if (!cache.has(name)) {
    let values = [];
    try {
      const type = BlockStates.get(name);
      values = type ? readArray(type.validValues) : [];
    } catch {
      values = [];
    }
    cache.set(name, values);
  }
  return cache.get(name);
}

// Bloğun o özellik için gerçekten kabul ettiği değerler: istenen değer geri okunuyorsa geçerlidir.
function measureValues(id, name, candidates, errors) {
  const accepted = [];
  for (const candidate of candidates) {
    try {
      const resolved = BlockPermutation.resolve(id, { [name]: candidate }).getAllStates();
      if (resolved[name] === candidate) {
        accepted.push(candidate);
      }
    } catch (error) {
      errors.push(`${name}=${JSON.stringify(candidate)}: ${error}`);
    }
  }
  return accepted;
}

// Durum anahtarı ad sırasına göre kurulur: getAllStates'in alan sırası garanti değil.
function stateKey(names, states) {
  return names.map((name) => `${name}=${JSON.stringify(states[name])}`).join(",");
}

function* enumerate(emit, plan) {
  const cache = new Map();
  const planned = new Map(plan.map((entry) => [entry.id, entry]));
  let resolves = 0;

  for (const type of BlockTypes.getAll()) {
    const errors = [];
    let base;
    try {
      base = BlockPermutation.resolve(type.id);
    } catch (error) {
      emit("PALETTE", { id: type.id, error: String(error) });
      yield;
      continue;
    }

    const fromPlan = planned.get(type.id);
    const names = (fromPlan ? fromPlan.properties : Object.keys(base.getAllStates())).slice().sort();
    if (names.length === 0) {
      emit("PALETTE", { id: type.id, states: [{}], ...(fromPlan ? {} : { unplanned: true }) });
      yield;
      continue;
    }

    const lists = [];
    for (const name of names) {
      // Planda değer verilmişse ölçüm atlanır: kullanımdan kalkmış bloklarda geri okuma aralığı daraltıyor.
      const fixed = fromPlan && fromPlan.values ? fromPlan.values[name] : undefined;
      if (fixed) {
        lists.push(readArray(fixed));
        continue;
      }
      const candidates = globalValues(name, cache);
      if (candidates.length === 0) {
        errors.push(`${name}: deger listesi yok`);
        lists.push([base.getAllStates()[name]]);
        continue;
      }
      lists.push(measureValues(type.id, name, candidates, errors));
      resolves += candidates.length;
      if (resolves % RESOLVES_PER_YIELD < candidates.length) {
        yield;
      }
    }

    let total = 1;
    for (const list of lists) {
      total *= Math.max(list.length, 1);
    }
    if (total > MAX_COMBINATIONS) {
      emit("PALETTE", { id: type.id, states: [base.getAllStates()], error: `cok fazla kombinasyon: ${total}` });
      yield;
      continue;
    }

    const seen = new Set();
    const states = [];
    const counters = new Array(names.length).fill(0);
    for (let n = 0; n < total; n++) {
      const combination = {};
      for (let i = 0; i < names.length; i++) {
        combination[names[i]] = lists[i][counters[i]];
      }
      try {
        const resolved = BlockPermutation.resolve(type.id, combination).getAllStates();
        const picked = {};
        for (const name of names) {
          picked[name] = resolved[name];
        }
        const key = stateKey(names, picked);
        if (!seen.has(key)) {
          seen.add(key);
          states.push(picked);
        }
      } catch (error) {
        errors.push(`${stateKey(names, combination)}: ${error}`);
      }
      for (let i = names.length - 1; i >= 0; i--) {
        if (++counters[i] < lists[i].length) {
          break;
        }
        counters[i] = 0;
      }
      if (++resolves % RESOLVES_PER_YIELD === 0) {
        yield;
      }
    }
    emit("PALETTE", {
      id: type.id,
      states,
      ...(fromPlan ? {} : { unplanned: true }),
      ...(errors.length > 0 ? { error: errors.slice(0, 5).join(" | ") } : {}),
    });
    yield;
  }
}

export function dumpPalette(emit, onDone, plan) {
  const job = enumerate(emit, plan ?? []);
  system.runJob((function* () {
    yield* job;
    onDone();
  })());
}
