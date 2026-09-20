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
python3 tools/vanilla-oracle/run_oracle.py --mode palette          # kanonik blok paleti
```

Çıktı: `server/src/test/resources/vanilla-oracle/<bds-sürümü>.json`. Senaryolardan biri kurulamazsa ya da sonucu
eksikse betik tabloyu yine yazar ama **1 ile çıkar** ve hataları listeler.

## Kayıt dökümü (`--mode dump`)

Endstone DevTools yalnızca Windows'ta çalıştığı için Allay'in veri güncellemesinde BDS'ten alınması gereken
değerlerin Script API ile okunabilen kısmını döker:

```bash
python3 tools/vanilla-oracle/run_oracle.py --mode dump --bds-version 1.26.51.1 \
    --output data/resources/unpacked/staging-1.26.50/bds_registry_dump.json
```

- **`blocks`**: her blok türünün varsayılan durumu (`BlockPermutation.resolve`), etiketleri ve çeviri anahtarı.
- **`items`**: `ItemTypes.getAll()` içindeki her eşyanın yığın sayısı, etiketleri, dayanıklılığı, büyü yuvaları,
  yiyecek bileşeni ve çeviri anahtarı.
- **`fuel`**: her eşya bir fırının yakıt yuvasına konur (girdi 64 kırıktaş), fırının `lit_furnace` kaldığı tik
  sayısı ölçülür. İki tikte bir bakılır; `fuelCapTicks` (2500) üstü `capped` olarak işaretlenir.

Allay'in 26.30 Endstone verisiyle karşılaştırma (2026-09-17, BDS 1.26.51.1):
- varsayılan durum 1356 türün 1355'inde aynı (fark `minecraft:unknown`);
- blok etiketi 1356 türün 1281'inde aynı, kalan 75 26.50'nin yeni etiketleri (`cornerable_stairs`, `leaves`);
- iki tarafta da bulunan 1518 eşyada yığın sayısı ve dayanıklılık %100, yakıt süresi 1515/1515 aynı (3 eşya sınır
  üstü), etiketler 1514/1518 (farklar 26.50 etiketleri).

Dikkat:
- Script API etiketleri bazen önek olmadan döndürür (`wood`); karşılaştırmada `minecraft:` eklenir.
- Durum listesinde palette olmayan eski adlar olabilir (`wood_type`); varsayılan durum palet girdisiyle
  "palet anahtarlarının hepsi eşleşiyor" diye bulunur.
- `ItemTypes.getAll()` elde tutulamayan eşyaları vermez (26.50'de 2076 eşyanın 453'ü: çift yarım blok, duvar
  tabelası, eğitim eşyaları). Bunların değerleri içe aktarma aracında türetilir.

## Bağlantı yüzü tablosu (`--mode faces`)

Çitin ve cam panelin/parmaklığın bir bloğun hangi yüzüne bağlandığı Allay'in şekil verisinden çıkmıyor (ruh kumu,
çamur, bal bloğu bağlanır; kaktüs, yaprak, basamak bağlanmaz). Bu kip paletteki **her durumu** (26.50'de 22091) ölçer:

```bash
python3 tools/vanilla-oracle/run_oracle.py --mode faces --bds-version 1.26.51.1 \
    --output data/resources/unpacked/bds_connection_faces.json
./gradlew :data:runMain -PmainClass=org.allaymc.data.importer.ConnectionFaceImport   # block_connection_faces.json
```

- Her durum bir hücrenin ortasına, dört yanına sonda konur: önce `nether_brick_fence`, sonra `iron_bars`. 6 tick sonra
  sondanın ortaya bakan `minecraft:connection_*` değeri okunur. Hücreler 3 blok arayla, 52×52'lik partilerle kurulur.
- Bütün partiler **tek bir 160×160 tickingarea**'da çalışır; partiler arasında alan `fill ... air` ile temizlenir.
  Her parti için ayrı uzak alan açmak BDS 1.26.51.1'de üçüncü alanda takıldı (alan hiç yüklenmedi).
- Sıvılar 12 blok arayla ayrı partide ölçülür, akıp komşu hücreyi bozmasın.
- **Köşeli merdiven tek başına kararlı değil**: BDS köşeyi komşulara bakıp sıfırlıyor. Bu durumlar köşeyi oluşturan
  komşu merdivenle birlikte kurulur (komşu, altın tablodaki `STAIRS` senaryolarından seçilir) ve yalnızca boş kalan üç
  yan ölçülür; tabloda komşunun yanı `-`.
- Ham tablo satırı `[ad, çit sondası, panel sondası]`; hücre `"KDGB"` (1 bağlandı, 0 bağlanmadı, - ölçülmedi). Blok
  ölçüm sırasında başka duruma döndüyse (duvar bağlantısı, desteksiz meşale, ölen mercan) hücre
  `["KDGB", son durumun palet sırası]` olur ve yüzler o son duruma aittir. Türetme kuralları `ConnectionFaceImport`
  javadoc'unda.
- Ölçüm tekrarlanabilir: iki çalıştırmada yüzler birebir aynı çıktı; yalnızca mercan bloğu ve sarmaşık gibi zamana
  bağlı değişen blokların "değişti" işareti oynuyor, türetme bundan etkilenmiyor.
- 26.50 bulgusu: iç köşeli merdivende yalnızca arka yüz bağlanıyor (Java'da köşenin yan yüzü de dolu sayılır).

## Kanonik blok paleti (`--mode palette`)

Palet (blok türü başına bütün durumlar) ağda gelmiyor — istemci kendi içinde taşıyor — bu yüzden bugüne kadar
CloudburstMC/Data'nın `block_palette.nbt`'sinden alınıyordu. Bu kip onu oyunun kendisinden üretir:

```bash
python3 tools/vanilla-oracle/run_oracle.py --mode palette --bds-version 1.26.51.1 \
    --output tools/vanilla-oracle/palette-1.26.51.1.json
```

İki kaynağı birleştirir:

- **Özellik adları** Mojang'ın kendi meta verisinden (`unpacked/mojang-blocks.json`, `data_items[].properties`).
  Betik API'sinin `getAllStates()`'i eski takma adları da veriyor (`acacia_wood`'da `wood_type`, `allium`'da
  `flower_type`); palette onlar yok, bu yüzden adlar oradan okunmaz.
- **Özellik değerleri** blok başına ölçülür. Genel liste `age` için 0–15 der ama kakao 0–2 kullanır;
  `BlockPermutation.resolve` aralık dışı değeri **sessizce varsayılana düşürdüğü** için geri okuma testi
  (istenen === okunan) o bloğun gerçek kümesini verir.

Mojang'ın listesi 1463 blok kapsıyor, oyunda 1477 tür var. Kalan 14'ünde betik kendi ad listesini kullanır;
dördü (`chalkboard`, `deprecated_anvil`, `deprecated_purpur_block_1`/`_2`) takma ad ya da dar aralık verdiği için
`PALETTE_OVERRIDES` ile elle yazılır — kullanımdan kalkmış bloklar, sürümle değişmiyorlar.

**Doğrulama (2026-09-20, BDS 1.26.51.1):** üretilen 22091 durum, CloudburstMC paletiyle **birebir** aynı
(fark yok, iki yönde de). Yani palet için dış kaynağa gerek kalmadı.

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
- **Zamanlamaya bağlı sonuç:** `S9/trip_wire_hook/d0` senaryosunda ipin `powered_bit` değeri çalıştırmadan
  çalıştırmaya değişebiliyor (2026-09-17 tekrarında `true`, altın tabloda `false`); kancası sökülen ip okuma anında
  geçici durumda olabiliyor. Bu değeri kullanan test onu karşılaştırmamalı. Diğer 479 senaryo iki çalıştırmada aynı.
- Son çalıştırmanın BDS günlüğü `<cache-dir>/last_run.log`.
