package org.allaymc.server.entity.data;

import java.util.Map;

/**
 * PocketMine'in diske yazdigi eski varlik kayit adlari.
 *
 * <p>PocketMine her vanilla varligi <b>ilk kayit adiyla</b> yazar
 * ({@code EntityFactory::register} notu: "The first save name in the
 * $saveNames array will be used when saving the entity to disk"), ve o ilk ad
 * modern {@code minecraft:*} kimligi degil eski Bedrock adidir: tablo
 * {@code Painting}, dusen esya {@code Item}, ok {@code Arrow} olarak
 * kaydedilir.</p>
 *
 * <p>Bu adlar motorun kayit defterinde yok; eslemesiz okuma her birini
 * "bilinmeyen varlik" sayip <b>atlar</b> ve chunk yeniden yazilirken varlik
 * kalici olarak kaybolur. Tablo PocketMine'in kendi kayit listesinden
 * cikarilmistir.</p>
 */
public final class LegacyEntityNames {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("AreaEffectCloud", "minecraft:area_effect_cloud"),
            Map.entry("Arrow", "minecraft:arrow"),
            Map.entry("Egg", "minecraft:egg"),
            Map.entry("EnderCrystal", "minecraft:ender_crystal"),
            Map.entry("ThrownEnderpearl", "minecraft:ender_pearl"),
            Map.entry("ThrownExpBottle", "minecraft:xp_bottle"),
            Map.entry("XPOrb", "minecraft:xp_orb"),
            Map.entry("FallingSand", "minecraft:falling_block"),
            Map.entry("Item", "minecraft:item"),
            Map.entry("Minecart", "minecraft:minecart"),
            Map.entry("ChestMinecart", "minecraft:chest_minecart"),
            Map.entry("HopperMinecart", "minecraft:hopper_minecart"),
            Map.entry("Painting", "minecraft:painting"),
            Map.entry("PrimedTnt", "minecraft:tnt"),
            Map.entry("PrimedTNT", "minecraft:tnt"),
            Map.entry("Snowball", "minecraft:snowball"),
            Map.entry("Bee", "minecraft:bee"),
            Map.entry("Squid", "minecraft:squid"),
            Map.entry("Villager", "minecraft:villager"),
            Map.entry("Zombie", "minecraft:zombie")
    );

    private LegacyEntityNames() {
    }

    /**
     * Eski kayit adini modern kimlige cevirir.
     *
     * @param savedName NBT'deki {@code identifier} degeri
     * @return modern kimlik; ad eski listede yoksa {@code null}
     */
    public static String resolve(String savedName) {
        return savedName == null ? null : ALIASES.get(savedName);
    }

    /**
     * @return eski ad -> modern kimlik eslemesi
     */
    public static Map<String, String> aliases() {
        return ALIASES;
    }
}
