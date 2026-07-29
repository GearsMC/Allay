package org.allaymc.server.network.protocol.v2168;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.player.Player;
import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v1001.PacketEncoder_v1001;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;

import java.util.Collection;

/**
 * GearsMC fork: 1.26.40 (v2168) icin paket kodlayici.
 *
 * <p>v1001'den iki paket farkli kurulur. Geri kalan her sey degismedigi icin
 * ustteki surumden miras alinir.</p>
 */
public class PacketEncoder_v2168 extends PacketEncoder_v1001 {

    public PacketEncoder_v2168(ProtocolData data) {
        super(data);
    }

    /**
     * Tarifleri v2168'in turlere ayrilmis listelerine dagitir.
     *
     * <p>1.26.40'a kadar tum tarifler tek bir {@code craftingData} dizisindeydi ve
     * her kaydin basinda turu yaziliydi. v2168 bunu tur basina ayri diziye boldu:
     * {@code CraftingDataSerializer_v2168} artik yalnizca o listeleri yaziyor —
     * duz listeye eklemek sessizce bos bir tarif kitabi uretir, hata vermez.</p>
     *
     * <p>{@code FURNACE} ve {@code FURNACE_DATA} v2168'in bu paketinde artik yer
     * almiyor; o tarifler atlanir.</p>
     *
     * @return v2168 istemcisine gonderilecek tarif paketi
     */
    @Override
    public CraftingDataPacket encodeCraftingData() {
        var packet = super.encodeCraftingData();

        for (RecipeData recipe : packet.getCraftingData()) {
            switch (recipe.getType()) {
                case SHAPED -> packet.getShapedData().add((ShapedRecipeData) recipe);
                case SHAPELESS -> packet.getShapelessData().add((ShapelessRecipeData) recipe);
                case MULTI -> packet.getMultiData().add((MultiRecipeData) recipe);
                case SHULKER_BOX -> packet.getShapelessUserData().add((ShapelessRecipeData) recipe);
                case SHAPELESS_CHEMISTRY -> packet.getShapelessChemistryData().add((ShapelessRecipeData) recipe);
                case SHAPED_CHEMISTRY -> packet.getShapedChemistryData().add((ShapedRecipeData) recipe);
                case SMITHING_TRANSFORM -> packet.getSmithingTransformData().add((SmithingTransformRecipeData) recipe);
                case SMITHING_TRIM -> packet.getSmithingTrimData().add((SmithingTrimRecipeData) recipe);
                case FURNACE, FURNACE_DATA -> {
                    // v2168 firin tariflerini bu pakette tasimiyor.
                }
            }
        }
        // Duz liste artik yazilmiyor; birakilirsa yalnizca bellek isgal eder.
        packet.getCraftingData().clear();
        return packet;
    }

    /**
     * Oyuncu listesi girdilerine kendi eylemlerini yazar.
     *
     * <p>v2168'de eylem paket seviyesinden girdi seviyesine tasindi.
     * {@code PlayerListSerializer_v2168} girdi eylemi {@code null} ise
     * {@code NullPointerException} atiyor — yani eksik birakmak sessiz degil,
     * dogrudan baglanti kirilmasi demek.</p>
     *
     * @param players    etkilenen oyuncular
     * @param add        ekleme mi silme mi
     * @param trustSkins istemci derileri guvenilir saysin mi
     * @return eylemi girdilere de yazilmis paket
     */
    @Override
    public PlayerListPacket encodePlayerList(
            Collection<? extends Player> players,
            boolean add,
            boolean trustSkins
    ) {
        return stampEntryActions(super.encodePlayerList(players, add, trustSkins));
    }

    @Override
    public Collection<BedrockPacket> encodePlayerSkin(EntityPlayer player, boolean trustSkin) {
        var packets = super.encodePlayerSkin(player, trustSkin);
        for (var packet : packets) {
            if (packet instanceof PlayerListPacket listPacket) {
                stampEntryActions(listPacket);
            }
        }
        return packets;
    }

    /**
     * Paket seviyesindeki eylemi tum girdilere kopyalar.
     *
     * @param packet islenecek paket
     * @return ayni paket
     */
    private static PlayerListPacket stampEntryActions(PlayerListPacket packet) {
        if (packet == null) {
            return null;
        }
        for (var entry : packet.getEntries()) {
            if (entry.getAction() == null) {
                entry.setAction(packet.getAction());
            }
        }
        return packet;
    }
}
