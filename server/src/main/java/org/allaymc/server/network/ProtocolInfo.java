package org.allaymc.server.network;

import lombok.experimental.UtilityClass;
import org.allaymc.api.utils.SemVersion;
import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.ProtocolRegistry;
import org.allaymc.updater.block.BlockStateUpdater;
import org.allaymc.updater.block.BlockStateUpdater_1_21_110;
import org.allaymc.updater.item.ItemStateUpdater;
import org.allaymc.updater.item.ItemStateUpdater_1_26_20;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168_hotfix4;
import org.cloudburstmc.protocol.bedrock.codec.v818.Bedrock_v818;

/**
 * This class contains information about the current protocol version.
 *
 * @author daoge_cmd
 */
@UtilityClass
public final class ProtocolInfo {

    /**
     * Feature version is the version of the game from which vanilla features will be used.
     */
    // NOT: bilerek v2168 DEGIL, ama bu sabit oyun verisini SECMIYOR — yalnizca
    // beyan ediyor. Istemciye giden blok/esya tanimlari Registries.BLOCKS ve
    // Registries.ITEMS'tan uretilir (bkz. Protocol#createBlockDefinitions,
    // #createItemDefinitions); o kayit defterleri de data/resources altindaki
    // statik dosyalardan yuklenir. FEATURE_VERSION o zincirin hicbir yerinde
    // gecmez, dolayisiyla burayi tek basina yukseltmek hicbir veriyi
    // degistirmez — sadece asagidaki damgayi yalan yapar. Once
    // data/resources yeniden uretilmeli (bkz. unpacked/staging-1.26.40/README.md).
    //
    // Sabitin tek gercek etkisi dunya deposu kapisi (WorldDataCodec): level.dat'a
    // NetworkVersion damgasini bu yazar ve bundan yeni damgali bir dunyayi acmayi
    // REDDEDER. Yani yukseltmek tek yonlu; damgalanan dunyalar eski degere geri
    // donuldugunde acilmaz.
    public static final BedrockCodec FEATURE_VERSION = Bedrock_v1001.CODEC;

    /**
     * Bedrock version of the most recent backwards-incompatible change to block states.
     * <p>
     * This is different from the current game version. It should match the nearest version
     * that has block state changes.
     */
    public static final SemVersion BLOCK_STATE_VERSION = new SemVersion(1, 21, 110, 26, 0);

    /**
     * The currently used block state updater instance.
     */
    public static final BlockStateUpdater BLOCK_STATE_UPDATER = BlockStateUpdater_1_21_110.INSTANCE;

    /**
     * The currently used item state updater instance.
     */
    public static final ItemStateUpdater ITEM_STATE_UPDATER = ItemStateUpdater_1_26_20.INSTANCE;

    /**
     * The encoded version number of the block state version.
     */
    public static final int BLOCK_STATE_VERSION_NUM = (BLOCK_STATE_VERSION.major() << 24) |
                                                      (BLOCK_STATE_VERSION.minor() << 16) |
                                                      (BLOCK_STATE_VERSION.patch() << 8) | BLOCK_STATE_VERSION.revision();

    /**
     * Returns the latest international codec.
     *
     * <p>Before the default protocol registry is installed, this returns the built-in bootstrap value.</p>
     *
     * @return the latest codec
     */
    public static BedrockCodec getLatestCodec() {
        if (ProtocolRegistry.hasDefault()) {
            return ProtocolRegistry.getDefault().getLatest(ClientVariant.INTERNATIONAL).getCodec();
        }
        // GearsMC fork: bootstrap degeri de v2168. Acilis banner'i registry kurulmadan
        // once basiliyor; upstream'in v1001'i burada kalirsa sunucu desteklemedigi bir
        // ust sinir bildirir ("1.26.30'a kadar") ve 1.26.40 oyuncusu yanlis bilgilenir.
        // Protocol_v2168 ile ayni kodek kullanilir ki banner'daki surum dizesi
        // registry kurulduktan sonraki gercek degerle ayni kalsin.
        return Bedrock_v2168_hotfix4.CODEC;
    }

    /**
     * Returns the oldest international codec.
     *
     * <p>Before the default protocol registry is installed, this returns the built-in bootstrap value.</p>
     *
     * @return the oldest codec
     */
    public static BedrockCodec getLowestCodec() {
        if (ProtocolRegistry.hasDefault()) {
            return ProtocolRegistry.getDefault().getLowest(ClientVariant.INTERNATIONAL).getCodec();
        }
        return Bedrock_v818.CODEC;
    }

    /**
     * Get the feature minecraft version.
     *
     * @return the feature minecraft version
     */
    public static SemVersion getFeatureMinecraftVersion() {
        return SemVersion.from(FEATURE_VERSION.getMinecraftVersion());
    }

    /**
     * Get the latest minecraft version.
     *
     * @return the latest minecraft version
     */
    public static SemVersion getLatestMinecraftVersion() {
        return SemVersion.from(getLatestCodec().getMinecraftVersion());
    }

    /**
     * Get the lowest minecraft version.
     *
     * @return the lowest minecraft version
     */
    public static SemVersion getLowestMinecraftVersion() {
        return SemVersion.from(getLowestCodec().getMinecraftVersion());
    }
}
