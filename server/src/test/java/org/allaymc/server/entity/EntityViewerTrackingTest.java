package org.allaymc.server.entity;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.data.EntityNameTag;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.player.Player;
import org.allaymc.api.world.WorldViewer;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bir varligi bir izleyiciye dogurma istegi birkac bagimsiz yoldan geliyor: varlik eklendiginde
 * varlik yoneticisinden, bir chunk oyuncuya ulastiginda {@code viewChunk}'tan ve varlik
 * hareket ettiginde chunk siniri kontrolunden. Bunlardan biri bir sonraki tick'e kuyruklaniyor.
 * Yani ayni varlik ayni izleyiciye gercekten iki kez sunuluyor, ve istemcinin zaten bildigi bir
 * calisma zamani kimligi icin ikinci bir ekleme paketi gitmesi istemcinin varligi dusurmesine yol
 * aciyor: mob canli ve tehlikeli kalirken gorunmez oluyor. Kurt gibi chunk sinirlarini sik gecen
 * moblar bunu surekli yasiyor.
 */
@ExtendWith(AllayTestExtension.class)
public class EntityViewerTrackingTest {

    @Test
    void spawningTwiceOnlySendsTheEntityOnce() {
        var wolf = EntityTypes.WOLF.createEntity(EntityInitInfo.builder().build());
        var viewer = mock(WorldViewer.class);

        wolf.spawnTo(viewer);
        wolf.spawnTo(viewer);

        verify(viewer, times(1)).viewEntity(wolf);
        assertEquals(1, wolf.getViewers().size());
    }

    @Test
    void despawningSomeoneWhoNeverSawItSendsNothing() {
        var wolf = EntityTypes.WOLF.createEntity(EntityInitInfo.builder().build());
        var viewer = mock(WorldViewer.class);

        wolf.despawnFrom(viewer);

        verify(viewer, never()).removeEntity(wolf);
    }

    @Test
    void anEntityCanBeSpawnedAgainAfterBeingDespawned() {
        var wolf = EntityTypes.WOLF.createEntity(EntityInitInfo.builder().build());
        var viewer = mock(WorldViewer.class);

        wolf.spawnTo(viewer);
        wolf.despawnFrom(viewer);
        wolf.spawnTo(viewer);

        verify(viewer, times(2)).viewEntity(wolf);
        verify(viewer, times(1)).removeEntity(wolf);
        assertTrue(wolf.getViewers().contains(viewer));
    }

    /**
     * Varlik hareketi compute thread havuzunda tickleniyor ve chunk sinirini gecmek o worker
     * thread'inden izleyici ekleyip cikariyor; ayni anda AI tick'ini calistiran diger worker'lar ve
     * ana thread tam da ayni seti hareketi, durumu ve eylemleri yayinlamak icin dolasiyor. Setin
     * buna dayanmasi gerek: senkronize olmayan bir set dolasmanin ortasinda hata firlatir ya da
     * sessizce bir izleyici kaybeder, ve kaybolan bir izleyici demek istemciye o varliktan bir daha
     * bahsedilmemesi, ustelik bir kaldirma da gonderilmemesi demek; mob o oyuncu icin oylece yok
     * olur.
     */
    @Test
    void viewersSurviveBeingChangedWhileBeingIterated() throws Exception {
        var wolf = EntityTypes.WOLF.createEntity(EntityInitInfo.builder().build());
        var steadyViewer = mock(WorldViewer.class);
        wolf.spawnTo(steadyViewer);

        var failure = new AtomicReference<Throwable>();
        var rounds = 2000;

        // Kurdu chunk sinirlari boyunca yuruten fizik thread'inin yerini tutar.
        var mutator = new Thread(() -> {
            try {
                for (int i = 0; i < rounds; i++) {
                    var viewer = mock(WorldViewer.class);
                    wolf.spawnTo(viewer);
                    wolf.despawnFrom(viewer);
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        // Izleyici setini dolasan her yayinin yerini tutar.
        var reader = new Thread(() -> {
            try {
                for (int i = 0; i < rounds; i++) {
                    wolf.forEachViewers(viewer -> {
                    });
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        mutator.start();
        reader.start();
        mutator.join();
        reader.join();

        assertNull(failure.get(), "izleyici seti dolasilirken degistirilmeye elverisli degil");
        assertTrue(wolf.getViewers().contains(steadyViewer),
                "hic ayrilmayan izleyici kargasa dindiginde hala orada olmali");
    }

    /**
     * Silahli moblar {@code spawnTo}'yu silahlarini ve zirhlarini da gondermek icin eziyor, bu
     * yuzden korumanin orada da gecerli olmasi gerek; aksi halde tekrarlanan dogurma yalnizca yari
     * yariya bastirilmis olur.
     */
    @Test
    void armedMobsAlsoIgnoreADuplicateSpawn() {
        var pillager = EntityTypes.PILLAGER.createEntity(EntityInitInfo.builder().build());
        var viewer = mock(WorldViewer.class);

        pillager.spawnTo(viewer);
        pillager.spawnTo(viewer);

        verify(viewer, times(1)).viewEntity(pillager);
        verify(viewer, times(1)).viewEntityHand(pillager);
    }

    @Test
    void viewerNameTagsAreIsolatedAndRestoreTheEntityDefault() {
        var fox = EntityTypes.FOX.createEntity(EntityInitInfo.builder().build());
        var firstController = mock(Player.class);
        var secondController = mock(Player.class);
        var firstViewer = mock(EntityPlayer.class);
        var secondViewer = mock(EntityPlayer.class);
        when(firstViewer.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(secondViewer.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        when(firstViewer.getController()).thenReturn(firstController);
        when(secondViewer.getController()).thenReturn(secondController);

        fox.setNameTag("global-name");
        fox.setNameTagAlwaysShow(false);
        fox.setNameTagForViewer(firstViewer, "first-name", true);

        assertEquals(new EntityNameTag("first-name", true), fox.getNameTagForViewer(firstViewer));
        assertEquals(new EntityNameTag("global-name", false), fox.getNameTagForViewer(secondViewer));
        assertEquals("global-name", fox.getNameTag());
        assertFalse(fox.isNameTagAlwaysShow());

        fox.spawnTo(firstController);
        fox.spawnTo(secondController);
        clearInvocations(firstController, secondController);

        fox.setNameTagForViewer(firstViewer, "updated-name", false);

        verify(firstController).viewEntityState(fox);
        verify(secondController, never()).viewEntityState(fox);
        assertEquals(new EntityNameTag("updated-name", false), fox.getNameTagForViewer(firstViewer));
        assertEquals(new EntityNameTag("global-name", false), fox.getNameTagForViewer(secondViewer));

        clearInvocations(firstController, secondController);
        assertTrue(fox.clearNameTagForViewer(firstViewer));
        assertFalse(fox.clearNameTagForViewer(firstViewer));

        verify(firstController).viewEntityState(fox);
        verify(secondController, never()).viewEntityState(fox);
        assertEquals(new EntityNameTag("global-name", false), fox.getNameTagForViewer(firstViewer));
    }
}
