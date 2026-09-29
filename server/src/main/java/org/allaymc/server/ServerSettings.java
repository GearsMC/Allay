package org.allaymc.server;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.data.Difficulty;

import java.util.UUID;

/**
 * ServerSettings store the settings of the server.
 *
 * @author daoge_cmd
 */
@SuppressWarnings("ALL")
@Getter
@Accessors(fluent = true)
public class ServerSettings extends OkaeriConfig {

    @CustomKey("generic-settings")
    private GenericSettings genericSettings = new GenericSettings();
    @CustomKey("network-settings")
    private NetworkSettings networkSettings = new NetworkSettings();
    @CustomKey("world-settings")
    private WorldSettings worldSettings = new WorldSettings();
    @CustomKey("entity-settings")
    private EntitySettings entitySettings = new EntitySettings();
    @CustomKey("storage-settings")
    private StorageSettings storageSettings = new StorageSettings();
    @CustomKey("resource-pack-settings")
    private ResourcePackSettings resourcePackSettings = new ResourcePackSettings();
    @CustomKey("bstats-settings")
    private BStatsSettings bStatsSettings = new BStatsSettings();

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class GenericSettings extends OkaeriConfig {

        // GearsMC: PocketMine server.properties ile ayni (motd=GearsMC Skyblock)
        @Comment("Sunucu listesinde görünen ad (MOTD). PocketMine'daki motd ile aynı.")
        private String motd = "GearsMC Skyblock";

        @Comment("İkinci satır MOTD. Genellikle yalnızca yerel ağ (LAN) listesinde görünür.")
        @CustomKey("sub-motd")
        private String subMotd = "GearsMC Skyblock";

        // GearsMC: PocketMine server.properties (max-players=100)
        @Comment("Aynı anda sunucuda bulunabilecek en fazla oyuncu sayısı (PocketMine: max-players).")
        @CustomKey("max-player-count")
        private int maxPlayerCount = 100;

        // GearsMC: PocketMine gamemode=SURVIVAL
        @Comment("Yeni oluşturulan bir dünyanın varsayılan oyun modu.")
        @Comment("Olası değerler: SURVIVAL (hayatta kalma), CREATIVE (yaratıcı), ADVENTURE (macera), SPECTATOR (izleyici)")
        @CustomKey("default-game-mode")
        private GameMode defaultGameMode = GameMode.SURVIVAL;

        // GearsMC: PocketMine difficulty=3 (zor)
        @Comment("Yeni oluşturulan bir dünyanın varsayılan zorluğu.")
        @Comment("Olası değerler: PEACEFUL (huzurlu), EASY (kolay), NORMAL (normal), HARD (zor)")
        @CustomKey("default-difficulty")
        private Difficulty defaultDifficulty = Difficulty.HARD;

        @Comment("Oyuncuların varsayılan yetki seviyesi.")
        @Comment("Olası değerler: VISITOR (ziyaretçi), MEMBER (üye), OPERATOR (operatör)")
        @CustomKey("default-permission")
        private String defaultPermission = "MEMBER";

        // GearsMC: PocketMine language=tur
        @Comment("Konsolun kullandığı dil (ör. tr_TR, en_US).")
        private LangCode language = LangCode.tr_TR;

        @Comment("Hata ayıklama kipi açıksa konsol daha ayrıntılı bilgi yazar.")
        private boolean debug = false;

        @Comment("Beyaz liste açıksa yalnızca listedeki oyuncular sunucuya girebilir.")
        @CustomKey("enable-whitelist")
        private boolean enableWhitelist = false;

        // GearsMC: sunucu VDS'te ekransiz calisiyor
        @Comment("Masaüstü kontrol paneli penceresini açar. Ekransız sunucuda (VDS) kapalı kalmalıdır.")
        @CustomKey("enable-gui")
        private boolean enableGui = false;

        @Comment("Hesaplama iş parçacığı havuzundaki en fazla iş parçacığı sayısı.")
        @Comment("Değer 0 veya daha küçükse işlemci çekirdeği sayısı kadar kullanılır.")
        @CustomKey("max-compute-thread-count")
        private int maxComputeThreadCount = 0;

        @Comment("Sentry, hata takibi ve performans izleme hizmetidir. Geliştirme sürümünde varsayılan olarak kapalıdır;")
        @Comment("buraya true yazarak zorla açabilirsiniz.")
        @CustomKey("force-enable-sentry")
        private boolean forceEnableSentry = false;
    }

    @Getter
    @Accessors(fluent = true)
    public static class NetworkSettings extends OkaeriConfig {
        @Comment("Sunucunun dinleyeceği IPv4 adresi (0.0.0.0 = tüm ağ arayüzleri).")
        private String ip = "0.0.0.0";

        @Comment("Sunucunun IPv4 portu (UDP). Bedrock varsayılanı 19132.")
        private int port = 19132;

        @Comment("IPv6 desteği açık mı. Açıkken IPv6 kullanan oyuncular da bağlanabilir.")
        private boolean enablev6 = true;

        @Comment("Sunucunun dinleyeceği IPv6 adresi. Yalnızca enablev6 true iken geçerlidir.")
        private String ipv6 = "::";

        @Comment("Sunucunun IPv6 portu (UDP). Yalnızca enablev6 true iken geçerlidir.")
        private int portv6 = 19133;

        @Comment("Xbox hesabı doğrulaması. Açıkken yalnızca Xbox'a giriş yapmış oyuncular girebilir.")
        @CustomKey("xbox-auth")
        private boolean xboxAuth = true;

        @Comment("Ağ trafiği şifrelemesi. Güvenlik için açık tutulması önemle önerilir.")
        @CustomKey("enable-network-encryption")
        private boolean enableNetworkEncryption = true;

        @Comment("Her oyuncu oturumu için ayrıntılı paket bilgisini loga yazar (yalnızca hata ayıklama için).")
        @CustomKey("debug-packets")
        private boolean debugPackets = false;

        @Comment("Ağ veri akışında kullanılan sıkıştırma algoritması. Olası değerler: ZLIB, SNAPPY")
        @Comment("ZLIB daha iyi sıkıştırır, SNAPPY daha hızlı çalışır.")
        @CustomKey("compression-algorithm")
        private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.ZLIB;

        @Comment("Ağ iş parçacığı sayısı. 0 yazılırsa sunucu sayıyı kendisi belirler.")
        @CustomKey("network-thread-number")
        private int networkThreadNumber = 0;

        @Comment("Bir seferde işlenebilecek en fazla paket sayısı.")
        @CustomKey("max-synced-packets-handle-count-once")
        private int maxSyncedPacketsHandleCountAtOnce = 128;

        @Comment("Bir RakNet tikinde (10 ms) her adresin gönderebileceği en fazla datagram paketi sayısı.")
        @Comment("Varsayılan 120'dir; geliştirme kipinde sunucu Integer.MAX_VALUE kullanır.")
        @CustomKey("raknet-packet-limit")
        private int raknetPacketLimit = 120;

        @Comment("Bir RakNet tikinde, sunucu gelen veriyi düşürmeye başlamadan önce işlenecek toplam datagram sayısı.")
        @Comment("Varsayılan 100000'dir (raknetPacketLimit * 0.56 * 1500 farklı bağlantı); geliştirme kipinde Integer.MAX_VALUE olur.")
        @CustomKey("raknet-global-packet-limit")
        private int raknetGlobalPacketLimit = 100000;

        @Comment("RakNet sunucu bağlantısının kullanabileceği en büyük MTU.")
        @Comment("İnternet en fazla 1492 MTU destekler ama bu paket parçalanmasında sorun çıkarabilir.")
        @Comment("Varsayılan 1400'dür.")
        @CustomKey("raknet-max-mtu")
        private int raknetMaxMtu = 1400;

        @Comment("Bir istemcinin giriş aşamasında kalabileceği en uzun süre (birim: gt, 20 gt = 1 sn).")
        @Comment("Kötü niyetle giriş aşamasında bekletilen çok sayıda sahte istemcinin sunucuyu tıkamasını önler.")
        @Comment("Kapatmak için değeri 0 veya daha küçük yapın.")
        @CustomKey("max-login-time")
        private int maxLoginTime = 90 * 20;

        @Comment("Kodlama koruması, istemcinin büyük çöp veri göndermesini engeller.")
        @Comment("Açık tutulması önerilir; ancak istemciler yanlış değerlendirme yüzünden atılıyorsa")
        @Comment("(genellikle görünüm/skin değişirken) bu korumayı kapatmak sorunu çözer.")
        @CustomKey("enable-encoding-protection")
        private boolean enableEncodingProtection = true;

        @Comment("NetEase (Çin) Minecraft istemcileri için destek.")
        @CustomKey("netease-client-support")
        private boolean neteaseClientSupport = false;

        @Comment("true ise yalnızca NetEase istemcileri sunucuya girebilir.")
        @Comment("Bu ayar yalnızca netease-client-support açıkken geçerlidir.")
        @CustomKey("only-allow-netease-client")
        private boolean onlyAllowNeteaseClient = false;

        @Comment("Sıkıştırması açılmış paket verisi için en büyük boyut (bayt). Varsayılan 50 MB (52428800).")
        @Comment("Büyük paketlerde açma hatası alıyorsanız artırın.")
        @CustomKey("max-decompressed-bytes")
        private int maxDecompressedBytes = 1024 * 1024 * 50;

        // GearsMC sapması: varsayılan açık. Oyuncu aynı bölgeye döndüğünde chunk verisi yeniden gönderilmiyor,
        // yalnızca blob özeti gidiyor; istemcide yoksa NAK ile isteniyor. Açamayacağı durum yok: istemci
        // desteklemiyorsa (ClientCacheStatusPacket false) ya da blob işlemi açılamazsa paket sıkıştırılmamış
        // haliyle gönderiliyor (PacketEncoder_v766, cachingEnabled=false dalı).
        @Comment("Blob özetleriyle istemci tarafı chunk önbelleğini açar; oyuncular aynı bölgeye döndüğünde bant genişliğini ciddi biçimde azaltır.")
        @Comment("Bu açıkken oyuncular sunucudan atılıyorsa network-settings.enable-encoding-protection ayarını false yapın.")
        @CustomKey("enable-client-chunk-cache")
        private boolean enableClientChunkCache = true;

        // GearsMC sapması: 4096 yerine 8192. Her ada ayrı bir dünya olduğu için oyuncular arasında blob paylaşımı
        // düşük; bir oyuncunun 8 chunk görüş alanı kabaca 1500-1700 blob tutuyor, 4096 üç oyuncuda dolup LRU'ya
        // düşüyordu. 8192 blob ~20 MB yer kaplar.
        @Comment("Tüm oyuncular arasında paylaşılan önbellekteki en fazla chunk blob sayısı.")
        @Comment("Bir blob genellikle 1-4 KB'tır. Yüksek değer daha çok bellek kullanır ama isabet oranını artırır.")
        @Comment("Öneri: küçük sunucular için 4096, büyük sunucular için 8192-16384.")
        @CustomKey("max-chunk-cache-blobs")
        private int maxChunkCacheBlobs = 8192;

        public enum CompressionAlgorithm {
            ZLIB,
            SNAPPY
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class WorldSettings extends OkaeriConfig {

        @Comment("Chunk yükleyicisinden (oyuncudan) ne kadar uzaktaki chunk'ların tiklenmeye devam edeceğini belirler.")
        @CustomKey("tick-radius")
        private int tickRadius = 4;

        @Comment("Chunk yükleyicisinden (oyuncudan) ne kadar uzaktaki chunk'ların yükleneceğini ve gönderileceğini belirler (chunk cinsinden).")
        @CustomKey("view-distance")
        private int viewDistance = 8;

        @Comment("Bir tikte (chunk yükleyici başına) gönderilebilecek en fazla chunk sayısı.")
        @CustomKey("chunk-max-send-count-per-tick")
        private int chunkMaxSendCountPerTick = 16;

        @Comment("Alt-chunk gönderim sistemini kullanır.")
        @CustomKey("use-sub-chunk-sending-system")
        private boolean useSubChunkSendingSystem = false;

        @Comment("Chunk gönderim stratejisi. Olası değerler: ASYNC (eşzamansız), SYNC (eşzamanlı)")
        @CustomKey("chunk-sending-strategy")
        private ChunkSendingStrategy chunkSendingStrategy = ChunkSendingStrategy.ASYNC;

        @Comment("Sunucuya giren istemciye gönderilmesi gereken en az chunk sayısı.")
        @Comment("Değeri düşürmek girişi hızlandırabilir; ancak çok düşükse istemci çok sayıda yüklenmemiş chunk görebilir.")
        @CustomKey("fully-join-chunk-threshold")
        private int fullyJoinChunkThreshold = 30;

        @Comment("Chunk yükleyicisi kalmayan bir chunk'ın bellekte ne kadar kalacağı (gt, 20 gt = 1 sn).")
        @CustomKey("remove-unused-full-chunk-cycle")
        private int removeUnusedFullChunkCycle = 60 * 20;

        @Comment("Yarım (proto) bir chunk'ın bellekte ne kadar kalacağı (gt, 20 gt = 1 sn).")
        @CustomKey("remove-unused-proto-chunk-cycle")
        private int removeUnusedProtoChunkCycle = 30 * 20;

        @Comment("true ise sunucu doğuş noktası çevresindeki chunk'ları yüklü tutar; bu, sunucuya girişi hızlandırır.")
        @Comment("Ancak bellek kullanımını artırır. Ayrıca sahte bir chunk yükleyici eklendiği için her dünyada bu chunk'lar")
        @Comment("yakında gerçek oyuncu olmasa bile sürekli tiklenir (ekin büyümesi, kızıltaş vb.). Çok sayıda küçük/ayrı dünyası olan")
        @Comment("sunucular (ör. oyuncu başına ada) boş dünyaların doğuş noktası çevresinde sonsuza dek çalışmaması için bunu false bırakmalıdır.")
        @CustomKey("load-spawn-point-chunks")
        private boolean loadSpawnPointChunks = false;

        @Comment("Doğuş noktası çevresinde kaç chunk yarıçapında yükleneceğini belirler.")
        @CustomKey("spawn-point-chunk-radius")
        private int spawnPointChunkRadius = 3;

        @Comment("true ise aynı dünyadaki boyutlar dünya tikinde paralel olarak tiklenir.")
        @CustomKey("tick-dimension-in-parallel")
        private boolean tickDimensionInParallel = true;

        @Comment("Boyut başına en fazla ışık güncelleme sayısı. Aşılırsa çok bellek harcamamak için")
        @Comment("yeni yüklenen chunk'lardaki ışık hemen hesaplanmaz.")
        @CustomKey("max-light-update-count")
        private int maxLightUpdateCountPerDimension = 1280000;

        public enum ChunkSendingStrategy {
            ASYNC,
            SYNC
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class EntitySettings extends OkaeriConfig {

        @Comment("Varlık fizik motoru ayarları.")
        @Comment("Ne yaptığınızı bilmiyorsanız değiştirmeyin!")
        @CustomKey("physics-engine-settings")
        private PhysicsEngineSettings physicsEngineSettings = new PhysicsEngineSettings();

        @Getter
        @Accessors(fluent = true)
        public static class PhysicsEngineSettings extends OkaeriConfig {
            @Comment("Hareket bu değerin altına düşerse sıfırlanır.")
            @CustomKey("motion-threshold")
            private float motionThreshold = 0.003f;

            @Comment("Blok içinde sıkışan eşya varlığının ne kadar hızlı dışarı itileceğini belirler.")
            @CustomKey("block-collision-motion")
            private float blockCollisionMotion = 0.2f;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class StorageSettings extends OkaeriConfig {
        @Comment("false yapılırsa oyuncu verisi kaydedilmez.")
        @CustomKey("save-player-data")
        private boolean savePlayerData = true;

        @Comment("Oyuncu verisinin otomatik kayıt döngüsü (gt, 20 gt = 1 sn; 6000 = 5 dk).")
        @CustomKey("player-data-auto-save-cycle")
        private int playerDataAutoSaveCycle = 20 * 60 * 5;

        @Comment("Chunk'ların otomatik kayıt döngüsü (gt, 20 gt = 1 sn; 6000 = 5 dk).")
        @CustomKey("chunk-auto-save-cycle")
        private int chunkAutoSaveCycle = 20 * 60 * 5;

        @Comment("Varlıkların otomatik kayıt döngüsü (gt, 20 gt = 1 sn). Tetiklendiğinde varlık yöneticisi")
        @Comment("yüklü olmayan chunk'lardaki kaydedilebilir tüm varlıkları bulup kaydeder.")
        @CustomKey("entity-auto-save-cycle")
        private int entityAutoSaveCycle = 20 * 60;
    }

    @Getter
    @Accessors(fluent = true)
    public static class ResourcePackSettings extends OkaeriConfig {

        @Comment("true ise kaynak paketleri otomatik şifrelenir.")
        @Comment("Bu açıkken Vibrant Visuals (Canlı Görseller) devre dışı kalır.")
        @CustomKey("auto-encrypt-packs")
        private boolean autoEncryptPacks = true;

        @Comment("Bir kaynak paketi parçasının en büyük boyutu (birim: KB).")
        @Comment("Değeri düşürmek, paketler birden çok istemciye gönderilirken ağ yükünü azaltabilir.")
        @Comment("Ancak paketlerin gönderilme süresini uzatabilir.")
        @CustomKey("max-chunk-size")
        private int maxChunkSize = 100; // 100KB, from BDS

        @Comment("true - oyuncu kaynak paketlerini kabul etmek zorundadır, etmezse sunucuya giremez.")
        @Comment("false - oyuncu kaynak paketlerini kabul etmeden de girebilir.")
        @CustomKey("force-resource-packs")
        private boolean forceResourcePacks = false;

        @Comment("true ise istemcinin kendi kaynak paketlerine izin verilir.")
        @CustomKey("allow-client-resource-packs")
        private boolean allowClientResourcePacks = false;

        @Comment("true ise tüm görünümler (skin) güvenilir olarak işaretlenir.")
        @CustomKey("trust-all-skins")
        private boolean trustAllSkins = true;

        @Comment("true ise Vibrant Visuals (Canlı Görseller) devre dışı bırakılır.")
        @CustomKey("disable-vibrant-visuals")
        private boolean disableVibrantVisuals = false;

        // TODO: URL packs configuration
    }

    @Getter
    @Accessors(fluent = true)
    public static class BStatsSettings extends OkaeriConfig {
        @Comment("bStats (https://bStats.org), eklenti yazarları için kaç kişinin eklentiyi kullandığı ve toplam oyuncu sayısı gibi")
        @Comment("temel bilgileri toplar. Açık bırakmanız önerilir; rahat değilseniz kapatabilirsiniz. Ölçümlerin performansa")
        @Comment("etkisi yoktur ve bStats'a gönderilen veri tamamen anonimdir.")
        private boolean enable = true;

        @Comment("bStats için bu sunucuya rastgele atanan kimlik.")
        @CustomKey("server-uuid")
        private String serverUUID = UUID.randomUUID().toString();

        @Comment("Başarısız bStats isteklerini loga yazar.")
        @CustomKey("log-failed-requests")
        private boolean logFailedRequests = false;

        @Comment("bStats'a gönderilen veriyi loga yazar.")
        @CustomKey("log-sent-data")
        private boolean logSentData = false;

        @Comment("bStats yanıtının durum metnini loga yazar.")
        @CustomKey("log-response-status-text")
        private boolean logResponseStatusText = false;
    }
}
