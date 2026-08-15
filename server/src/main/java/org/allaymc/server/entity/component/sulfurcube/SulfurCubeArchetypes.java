package org.allaymc.server.entity.component.sulfurcube;

import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.item.data.ItemTag;
import org.allaymc.api.item.data.ItemTags;

import java.util.Map;

/**
 * Hangi blogun sulfur kupune hangi kisiligi verdigini soyleyen esleme.
 *
 * <p>Esleme burada elle yazilmiyor: oyunun kendi verisinde her blok icin bir
 * {@code minecraft:sulfur_cube_archetype_*} item etiketi bulunuyor ve {@code items.json} ile
 * birlikte kayit defterine yukleniyor. Yani burasi yalnizca etiketi karsilik gelen kisilige
 * cevirir.</p>
 *
 * <p>Bu, aile tahminine dayali elle yazilmis bir listenin yerini aldi. Listenin sorunu 334 blogu
 * kimlik sonekleri ve blok etiketleriyle yakalamaya calismasiydi: hicbir kaliba uymayan bloklar
 * sessizce disarida kaliyordu; islak sunger, kalsit ve obsidyen emilemiyordu.</p>
 *
 * <p>Hicbir blok birden fazla arketip etiketi tasimadigi icin sorgu sirasi onemsiz.</p>
 *
 * <p>Etiketi olmayan blok emilemez; oyunun verisi bu konuda tek yetkili kaynak.</p>
 */
public final class SulfurCubeArchetypes {

    /**
     * Etiketten kisilige cevrim tablosu.
     *
     * <p>{@link SulfurCubeArchetype#NONE} burada yok, cunku o "blok yok" demek; karsiligi olan bir
     * etiket de yok.</p>
     */
    private static final Map<ItemTag, SulfurCubeArchetype> BY_TAG = Map.ofEntries(
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY, SulfurCubeArchetype.BOUNCY),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR, SulfurCubeArchetype.REGULAR),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY, SulfurCubeArchetype.SLOW_BOUNCY),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT, SulfurCubeArchetype.SLOW_FLAT),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT, SulfurCubeArchetype.FAST_FLAT),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT, SulfurCubeArchetype.LIGHT),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING, SulfurCubeArchetype.FAST_SLIDING),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING, SulfurCubeArchetype.SLOW_SLIDING),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY, SulfurCubeArchetype.STICKY),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE, SulfurCubeArchetype.HIGH_RESISTANCE),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE, SulfurCubeArchetype.EXPLOSIVE),
            Map.entry(ItemTags.SULFUR_CUBE_ARCHETYPE_HOT, SulfurCubeArchetype.HOT));

    private SulfurCubeArchetypes() {
    }

    /**
     * Bir blogun kupe verecegi kisiligi dondurur.
     *
     * @param blockType incelenecek blok turu
     * @return kisilik; blok emilemiyorsa {@code null}
     */
    public static SulfurCubeArchetype of(BlockType<?> blockType) {
        if (blockType == null) {
            return null;
        }

        // Etiketler bloga degil onun esya bicimine asili; esya bicimi olmayan bloklar (ornegin
        // yalnizca dunyada bulunan teknik bloklar) dogal olarak emilemez.
        var itemType = blockType.getItemType();
        if (itemType == null) {
            return null;
        }

        for (var entry : BY_TAG.entrySet()) {
            if (itemType.hasItemTag(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * @return blogun bir kup tarafindan emilebilir olup olmadigi
     */
    public static boolean isAbsorbable(BlockType<?> blockType) {
        return of(blockType) != null;
    }
}
