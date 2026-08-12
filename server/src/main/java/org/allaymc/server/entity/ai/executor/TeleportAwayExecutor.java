package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Varligi yakindaki rastgele guvenli bir noktaya isinlar; enderman'in yaralandiginda kacma sekli.
 *
 * <p>Rastgele birkac konum denenir ve varligin gercekten uzerinde durabilecegi ilki kullanilir;
 * hicbiri tutmazsa varlik oldugu yerde kalir ve davranis biter, boylece bir sonraki tick'te daha
 * dusuk oncelikli bir davranis devralir. Islemin tamami tek bir tick'te biter — bu executor asla
 * {@code true} dondurmez, yani davranis yuvasini hicbir zaman tutmaz.</p>
 */
public class TeleportAwayExecutor implements BehaviorExecutor {

    protected final int horizontalRange;
    protected final int verticalRange;
    protected final int maxAttempts;

    /**
     * Vanilla enderman menziline yakin bir isinlanma executor'u olusturur.
     */
    public TeleportAwayExecutor() {
        this(32, 16, 16);
    }

    /**
     * Bir isinlanma executor'u olusturur.
     *
     * @param horizontalRange isinlanilabilecek en fazla yatay mesafe (blok)
     * @param verticalRange isinlanilabilecek en fazla dikey mesafe (blok)
     * @param maxAttempts vazgecmeden once kac rastgele konum denenecegi
     */
    public TeleportAwayExecutor(int horizontalRange, int verticalRange, int maxAttempts) {
        this.horizontalRange = horizontalRange;
        this.verticalRange = verticalRange;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var destination = findDestination(entity);
        if (destination == null) {
            return false;
        }

        var dimension = entity.getDimension();
        var from = entity.getLocation();
        // Iki ucta da parcacik; yoksa mob oylece yok olmus gibi gorunur.
        dimension.addParticle(from.x(), from.y() + entity.getEyeHeight(), from.z(), SimpleParticle.ENDERMAN_TELEPORT);
        dimension.addSound(new Vector3d(from.x(), from.y(), from.z()), SimpleSound.TELEPORT);

        entity.teleport(destination);

        dimension.addParticle(destination.x(), destination.y() + entity.getEyeHeight(), destination.z(), SimpleParticle.ENDERMAN_TELEPORT);
        dimension.addSound(new Vector3d(destination.x(), destination.y(), destination.z()), SimpleSound.TELEPORT);

        // Izlenen rota artik eski konumu gosteriyor.
        EntityControlHelper.removeRouteTarget(entity);
        return false;
    }

    /**
     * Varligin uzerinde durabilecegi yakin bir rastgele konum secer; her deneme kullanilamaz bir
     * yere denk geldiyse {@code null} doner.
     */
    protected Location3d findDestination(EntityIntelligent entity) {
        var rand = ThreadLocalRandom.current();
        var dimension = entity.getDimension();
        var location = entity.getLocation();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double x = location.x() + rand.nextInt(-horizontalRange, horizontalRange + 1);
            double z = location.z() + rand.nextInt(-horizontalRange, horizontalRange + 1);
            double y = location.y() + rand.nextInt(-verticalRange, verticalRange + 1);

            if (entity.canStandSafely(x, y, z, dimension)) {
                return new Location3d(x, y, z, location.pitch(), location.yaw(), dimension);
            }
        }

        return null;
    }
}
