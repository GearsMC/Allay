package org.allaymc.api.item.component;

import org.allaymc.api.entity.interfaces.EntityPlayer;

import java.util.function.Predicate;

/**
 * Girdap (riptide) icin eklenti kapisi.
 *
 * <p>Sunucu kurallari girdabi yasakliyorsa (dunya grubu, hava sarti) buradaki
 * kapi {@code false} doner; motor girdap yerine mizragi firlatir — PHP
 * {@code canActivateRiptide} basarisizligindaki davranis. Varsayilan serbesttir;
 * GearsCore kurulumda baglar.</p>
 *
 * @author GearsMC fork
 */
public final class TridentRiptideGate {

    private static volatile Predicate<EntityPlayer> guard = player -> true;

    /**
     * Kapıyı bağlar; {@code null} serbest bırakır.
     *
     * @param predicate oyuncu -&gt; girdap serbest mi
     */
    public static void set(Predicate<EntityPlayer> predicate) {
        guard = predicate == null ? player -> true : predicate;
    }

    /**
     * Girdap bu oyuncu icin serbest mi.
     *
     * @param player oyuncu
     * @return serbestse {@code true}
     */
    public static boolean allows(EntityPlayer player) {
        return guard.test(player);
    }

    private TridentRiptideGate() {
    }
}
