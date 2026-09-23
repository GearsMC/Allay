package org.allaymc.api.eventbus.event.entity;

import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.item.ItemStack;

import java.util.List;

/**
 * Ölen varlığın ganimeti ve deneyimi dünyaya bırakılmadan hemen önce çağrılır.
 *
 * <p>Eklenti düşecek eşya listesini ve deneyim miktarını değiştirebilir; ikisi de
 * olaydan sonra olduğu gibi bırakılır. Liste {@code doMobLoot} kuralı kapalıysa boş,
 * deneyim yalnızca bir oyuncu öldürdüyse sıfırdan büyük gelir — motorun kendi
 * kuralları değişmez, eklenti yalnızca sonucu düzeltir. Yığılmış mob gibi tek varlıkta
 * birden çok mobu temsil eden sistemler ganimeti buradan çarpar.</p>
 */
@CallerThread(ThreadType.DIMENSION)
public class EntityLootEvent extends EntityEvent {

    private final List<ItemStack> drops;
    private int xp;

    /**
     * @param entity ölen varlık
     * @param drops  düşecek eşyalar; değiştirilebilir liste
     * @param xp     bırakılacak deneyim
     */
    public EntityLootEvent(Entity entity, List<ItemStack> drops, int xp) {
        super(entity);
        this.drops = drops;
        this.xp = Math.max(0, xp);
    }

    /**
     * @return düşecek eşyalar; doğrudan değiştirilebilir
     */
    public List<ItemStack> getDrops() {
        return drops;
    }

    /**
     * @return bırakılacak deneyim
     */
    public int getXp() {
        return xp;
    }

    /**
     * @param xp bırakılacak deneyim; negatifse sıfır sayılır
     */
    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }
}
