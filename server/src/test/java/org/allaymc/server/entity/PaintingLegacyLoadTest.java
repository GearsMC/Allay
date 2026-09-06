package org.allaymc.server.entity;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.data.PaintingType;
import org.allaymc.api.entity.interfaces.EntityPainting;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.NBTIO;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PocketMine'in yazdigi tablolarin okunabildigini dogrular.
 *
 * <p>Iki ayri engel vardi. Birincisi ad: PocketMine vanilla varliklari
 * <b>eski kayit adiyla</b> yaziyor ({@code EntityFactory}, "ilk kayit adi diske
 * yazilir"), yani tablo {@code Painting} olarak kaydediliyor; motorun kimligi
 * ise {@code minecraft:painting}. Ikincisi yon: motor bakis yonunu varligin
 * donusunden turetiyor ama PM donusu 0 yazip yonu {@code Direction} alaninda
 * sakliyor.</p>
 */
@ExtendWith(AllayTestExtension.class)
class PaintingLegacyLoadTest {

    private static NbtMap pocketMinePainting(String motive, int direction) {
        // Hub dunyasindaki gercek kayit bicimi.
        return NbtMap.builder()
                .putString("identifier", "Painting")
                .putList("Pos", org.cloudburstmc.nbt.NbtType.FLOAT, 268.0f, 99.0f, 284.0f)
                .putList("Rotation", org.cloudburstmc.nbt.NbtType.FLOAT, 0.0f, 0.0f)
                .putString("Motive", motive)
                .putByte("Direction", (byte) direction)
                .putByte("Facing", (byte) direction)
                .build();
    }

    @Test
    void legacyPaintingNameResolvesToThePaintingEntity() {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();

        var entity = NBTIO.getAPI().fromEntityNBT(dimension, pocketMinePainting("Kebab", 0));

        assertNotNull(entity, "eski kayit adiyla yazilmis tablo okunabilmeli");
        assertInstanceOf(EntityPainting.class, entity);
    }

    @Test
    void paintingKeepsItsMotive() {
        var painting = load(pocketMinePainting("Kebab", 0));

        assertEquals(PaintingType.KEBAB, painting.getPaintingType());
        assertEquals("Kebab", painting.saveNBT().getString("Motive"));
    }

    @Test
    void paintingKeepsTheDirectionPocketMineStored() {
        // PM donusu 0 yaziyor; yon yalnizca Direction alaninda duruyor.
        // Motor yonu donusten turettigi icin okuma sirasinda donus buna gore
        // ayarlanmali, yoksa butun tablolar ayni yone bakar.
        for (int direction = 0; direction < 4; direction++) {
            var painting = load(pocketMinePainting("Kebab", direction));

            assertEquals(BlockFace.fromHorizontalIndex(direction), painting.getHorizontalFace(),
                    "Direction " + direction + " icin bakis yonu korunmali");
            assertEquals(direction, painting.saveNBT().getByte("Direction"),
                    "Direction geri yazilmali");
        }
    }

    @Test
    void theFourElementPaintingsAreRecognised() {
        // Hub dunyasindaki tablonun deseni "Water"; dort element tablosu
        // motorun listesinde yoktu ve hepsi Kebab'a dusuyordu.
        for (String motive : new String[]{"Earth", "Wind", "Fire", "Water"}) {
            var painting = load(pocketMinePainting(motive, 0));

            assertEquals(motive, painting.getPaintingType().getTitle());
            assertEquals(motive, painting.saveNBT().getString("Motive"));
            assertFalse(painting.getPaintingType().isPlaceable(),
                    motive + " rastgele yerlestirme havuzuna girmemeli");
        }
    }

    @Test
    void anUnknownMotiveDoesNotBreakSaving() {
        // PocketMine hub'inda desen adi motorun listesinde olmayan bir tablo
        // var. PaintingType.fromTitle null donuyordu ve tur null kalinca
        // saveNBT NPE atiyordu; hata varlik otomatik kaydinda ciktigi icin
        // "Error while ticking world spawn" ile O DUNYANIN tum varlik kaydini
        // dusuruyordu.
        var painting = load(pocketMinePainting("bu_desen_yok", 0));

        assertNotNull(painting.getPaintingType(), "bilinmeyen desende varsayilan tur korunmali");
        assertEquals("Kebab", painting.saveNBT().getString("Motive"));
    }

    private static EntityPainting load(NbtMap nbt) {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        var painting = EntityTypes.PAINTING.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(268, 99, 284)
                .build());
        painting.loadNBT(nbt);
        return painting;
    }
}
