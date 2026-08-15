package org.allaymc.server.entity.ai.sensor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.ai.sensor.Sensor;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.server.entity.component.sulfurcube.SulfurCubeArchetypes;

/**
 * Emilebilir bir blok tasiyan en yakin varligi arar.
 *
 * <p>Sulfur kupu iki seyin pesine duser: yere dusmus emilebilir bir blok, ya da o blogu elinde
 * tutan bir oyuncu. Ikisi de "yaklas ve em" davranisini besledigi icin tek bir hafiza gozunde
 * toplaniyor; kupun ikisini ayirt etmesi gerekmiyor.</p>
 *
 * <p>Blok tasiyan bir kup zaten yerinde duruyor, dolayisiyla dolu kup icin tarama hic
 * yapilmiyor.</p>
 */
public class NearestAbsorbableSensor implements Sensor {

    protected final double range;
    protected final int period;

    /**
     * @param range aranacak en fazla mesafe (blok)
     * @param period iki tarama arasindaki tick sayisi
     */
    public NearestAbsorbableSensor(double range, int period) {
        this.range = range;
        this.period = period;
    }

    @Override
    public void sense(EntityIntelligent entity) {
        // Dolu ya da kucuk bir kup blok aramaz.
        if (entity instanceof EntitySulfurCubeBaseComponent cube
            && (!cube.isLarge() || cube.getAbsorbedBlock() != null)) {
            entity.getMemoryStorage().put(MemoryTypes.NEAREST_ABSORBABLE, null);
            return;
        }

        var loc = entity.getLocation();
        double rangeSq = range * range;

        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (var candidate : entity.getDimension().getEntities().values()) {
            if (candidate == entity || !candidate.isAlive() || !carriesAbsorbable(candidate)) {
                continue;
            }

            double distSq = loc.distanceSquared(candidate.getLocation());
            if (distSq > rangeSq || distSq >= nearestDistSq) {
                continue;
            }

            nearestDistSq = distSq;
            nearest = candidate;
        }

        entity.getMemoryStorage().put(MemoryTypes.NEAREST_ABSORBABLE,
                nearest != null ? nearest.getRuntimeId() : null);
    }

    protected boolean carriesAbsorbable(Entity candidate) {
        if (candidate instanceof EntityItem item) {
            return isAbsorbable(item.getItemStack());
        }

        if (candidate instanceof EntityPlayer player) {
            return isAbsorbable(player.getItemInHand());
        }

        return false;
    }

    protected boolean isAbsorbable(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        var blockType = itemStack.getItemType().getBlockType();
        return blockType != null && SulfurCubeArchetypes.isAbsorbable(blockType);
    }

    @Override
    public int getPeriod() {
        return period;
    }
}
