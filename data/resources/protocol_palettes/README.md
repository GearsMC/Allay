# Protokol blok paletleri

Her dosya, bir Bedrock sürümünün istemcisinin tanıdığı **resmi blok durumu listesidir** (gzip, big-endian NBT;
kökte `blocks` listesi, her girdide `name`, `states`, `version`). Sunucu blok kimliklerini hash'li gönderdiği
için istemci bir bloğu ancak ad + durum hash'i bu listede varsa çizer.

Allay'in blok verisi tek bir oyun sürümünü izler, istemciler ise 818–2193 arasında değişir. Her protokol
kendi paletini bildirir ve `BlockNetworkIdMapping` sunucu durumunu o istemcinin tanıdığı kimliğe çevirir.

Kaynak: [CloudburstMC/Data](https://github.com/CloudburstMC/Data) (Apache-2.0), `block_palette.nbt`.

| Dosya | Protokoller | Kaynak commit | SHA-1 |
|---|---|---|---|
| `1_21_90.nbt` | 818, 819 | `a8996250` (1.21.90) — 819'un `61d0fcb0` commit'indeki dosya birebir aynı | `5c8a7be1824d45aa221803dbdc00f5921a3d96e3` |
| `1_21_100.nbt` | 827 | `6b0c5029` | `290bd725c99038ce885459ba3d4cc3d397190d3e` |
| `1_21_111.nbt` | 844, 859, 860, 898, 924 | `10d55f78` — 859/898/924 commit'lerindeki dosyalar birebir aynı | `7c20bd073bebf8af91aea76a27b565225403ff63` |
| `1_26_10.nbt` | 944 | `96e312da` | `f2e9b5786c42bc5606444b1ac3613d2a57d6cfed` |
| `1_26_20.nbt` | 975 | `ca99088d` | `482096b1689de821e13d52713396b9b1c2c374d2` |
| `1_26_30.nbt` | 1001 | `6ce0e699` | `9d9cedc0fbe40cfca9f09e1063d6e8c2bdb5a922` |
| `1_26_40.nbt` | 2168, 2169 | `247e816d` | `9be2b645a50cbb2c83eb157d402f199b31030b7e` |
| `1_26_50.nbt` | 2192, 2193 | `7046791a` | `d5f0d3ac1615514decdb72240fa8af94f36f17f2` |

Yeni bir sürüm eklenirken dosya CloudburstMC/Data'nın o sürüm commit'inden alınır, bu tablo güncellenir ve
`BlockNetworkIdMappingTest`'teki beklenen palet tablosuna protokol eklenir.
