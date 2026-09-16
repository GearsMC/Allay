# Vanilla kahin

Bedrock Dedicated Server'ı (BDS) çalıştırıp senaryo tablosundaki blok dizilimlerini kurar ve **oyunun kendisinin
hesapladığı** blok durumlarını okur. Çıktı, sunucu tarafında aynı durumları hesaplayan kodun **altın tablosudur**.

İlk kullanım: 26.50'nin `minecraft:connection_*` (çit, cam panel, parmaklık, tuzak ipi) ve `minecraft:corner`
(merdiven) durumları. Hangi komşuya bağlanıldığını tahmin etmek yerine vanilladan ölçmek için yazıldı; Java, PM ve
PowerNukkitX'in kuralları birbirinden farklıydı. Yol haritası: `/root/gears/BLOK_VERISI_26_50_TODO.md` (6.3, Adım 2).

## Gereksinim

- Linux x86_64, Python 3.10+
- İnternet (BDS'i Mojang'dan indirmek için; yaklaşık 95 MB, açılınca 320 MB)
- BDS'in UDP portu boş olmalı. Varsayılan **19140**; Allay test sunucusunun 19132'siyle çakışmaz.
- Yaklaşık 500 MB boş bellek

## Kullanım

```bash
cd /root/gears/Allay
python3 tools/vanilla-oracle/build_scenarios.py          # scenarios.json'u yeniden üretir (senaryo değiştiyse)
python3 tools/vanilla-oracle/run_oracle.py               # Mojang'ın son Linux BDS sürümüyle
python3 tools/vanilla-oracle/run_oracle.py --bds-version 1.26.51.1
```

Çıktı: `server/src/test/resources/vanilla-oracle/<bds-sürümü>.json`. Senaryolardan biri kurulamazsa ya da sonucu
eksikse betik tabloyu yine yazar ama **1 ile çıkar** ve hataları listeler.

## Nasıl çalışır

1. BDS, `~/.cache/gears-vanilla-oracle/bedrock-server-<sürüm>/` altına indirilip açılır (`--cache-dir`).
   **BDS depoya konmaz**; kullanımı Minecraft son kullanıcı sözleşmesine tabidir.
2. `pack/` davranış paketi `development_behavior_packs/`'e kopyalanır; senaryolar hücre konumlarıyla birlikte
   `scripts/scenarios.js` olarak yazılır. Her senaryo 5×5'lik kendi hücresinde, düz dünyada y=-60'ta kurulur.
3. Her çalıştırmada boş bir düz dünya (`gears_vanilla_oracle`) açılır; alan `tickingarea` ile yüklü tutulur.
4. Betik bütün `place` yerleşimlerini uygular, 10 tick sonra `then` adımlarını, 20 tick sonra okumaları yapar ve her
   senaryo için `ORACLE RESULT {...}` satırı yazar. `run_oracle.py` bu satırları toplayıp BDS'i `stop` ile kapatır.

Yerleştirme `Block.setPermutation` ile yapılır. Fizibilite denemesinde `/setblock` ile aynı sonucu verdiği
doğrulandı; iki yolda da BDS bağlantıları ve komşu güncellemelerini kendisi hesaplıyor.

## Senaryo biçimi

`build_scenarios.py` kaynaktır; `scenarios.json` ondan üretilir ve depoda durur.

```json
{
  "id": "S1/fence_gate/north/closed",
  "question": "S1",
  "place": [
    { "at": [0, 0, 0], "name": "minecraft:oak_fence" },
    { "at": [1, 0, 0], "name": "minecraft:fence_gate", "states": { "minecraft:cardinal_direction": "north", "open_bit": false } }
  ],
  "then": [ { "at": [1, 0, 0], "name": "minecraft:air" } ],
  "read": [ [0, 0, 0] ]
}
```

Koordinatlar hücre merkezine göredir: +x doğu, −x batı, +z güney, −z kuzey. Blok ve durum adları Bedrock
paletindekiyle aynı olmalı (ör. meşe çit kapısı `minecraft:fence_gate`, meşe kapak `minecraft:trapdoor`,
jack o'lantern `minecraft:lit_pumpkin`). `read` verilmezse bütün yerleşimler okunur. Merkez dışında hücre içi
uzaklık en fazla 2 olmalı, yoksa komşu hücreyle etkileşir.

## Bilinen notlar

- Script API sürümü `pack/manifest.json`'da sabittir (`@minecraft/server` 2.10.0). Yeni BDS bu sürümü
  desteklemezse günlükte `[Scripting]` hatası çıkar; sürümü `bedrock-samples` `metadata/script_modules` altından seç.
- Script API bazı bloklarda palette olmayan sanal durumlar da döndürür (çitte `wood_type`). Tabloyu kullanan kod
  yalnızca ilgilendiği durumlara bakmalı.
- Desteksiz kalan bloklar kaldırılır: tuzak ipi kancası yalnızca arkasında dolu blok olan yönde durur, diğer
  yönlerde okumada `minecraft:air` görünür. Bu da vanilla davranışıdır.
- 26.50'de `online-mode=false` artık izin listesiyle birlikte reddediliyor; betik bu ayara dokunmaz.
- Son çalıştırmanın BDS günlüğü `<cache-dir>/last_run.log`.
