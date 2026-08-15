package org.allaymc.server.entity.component.cube;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityCubeBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.EntityLivingComponentImpl;
import org.allaymc.server.entity.component.event.CEntityCubeSizeChangeEvent;
import org.allaymc.server.entity.component.event.CEntityDieEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Kup moblarinin — balcik ve magma kupu — paylastigi canli varlik bileseni.
 *
 * <p>Kupun butun sayilari boyutundan turer: can boyutun karesi kadardir, yani buyuk bir kup
 * kucugunun on alti kati dayanikli olur. Olunce iki kucuk parcaya bolunur; en kucuk boyut
 * bolunmez ve esyasini duserek biter. Bolunmeyi burada yapmak sart, cunku parcalarin olen kupun
 * boyutunu ve konumunu bilmesi gerekiyor.</p>
 */
public class EntityCubeLivingComponentImpl extends EntityLivingComponentImpl {

    /** Olen kupun yerine dogan parca sayisi. */
    protected static final int SPLIT_COUNT = 2;

    /** Parcalarin birbirinden ayrilma mesafesi (blok). */
    protected static final double SPLIT_SPREAD = 0.5;

    @Dependency
    protected EntityCubeBaseComponent cubeBaseComponent;
    @Dependency
    protected EntityAIComponent aiComponent;

    protected final Supplier<EntityType<?>> selfType;
    protected final Supplier<ItemType<?>> dropSupplier;
    protected final boolean dropOnlyWhenSmallest;

    /**
     * @param selfType bolununce dogurulacak varlik turu; kupun kendi turu olmali. Gec cozulur
     *                 cunku {@code EntityTypes} alanlari turler kaydedilirken hala null'dur
     * @param dropSupplier oldugunde dusurdugu esya turu
     * @param dropOnlyWhenSmallest esyanin yalnizca en kucuk boyutta dusup dusmeyecegi; balcik boyle
     *                             davranir, magma kupu her boyutta duser
     */
    public EntityCubeLivingComponentImpl(Supplier<EntityType<?>> selfType,
                                         Supplier<ItemType<?>> dropSupplier,
                                         boolean dropOnlyWhenSmallest) {
        this.selfType = selfType;
        this.dropSupplier = dropSupplier;
        this.dropOnlyWhenSmallest = dropOnlyWhenSmallest;
    }

    /**
     * Cani kupun boyutuna gore ayarlar.
     *
     * <p>Boyut uc ayri anda belirlenebiliyor — dogal dogum, diskten yukleme, bolunme — ve ucunde de
     * temel bilesen bu olayi tetikliyor. Can yeniden hesaplandigi gibi dolduruluyor da; yoksa
     * bolunmeden cikan parcalar yarim canla dogardi.</p>
     */
    @EventHandler
    protected void onSizeChange(CEntityCubeSizeChangeEvent event) {
        // Can boyutun karesi: 1 / 4 / 16.
        setMaxHealth(event.getSize() * event.getSize());
        setHealth(getMaxHealth());
    }

    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        if (!super.attack(damage, ignoreCoolDown)) {
            return false;
        }

        // Kendisine vuran oyuncuyu kovalar; moblar arasindaki kazalar kin dogurmaz.
        if (damage.getAttacker() instanceof EntityPlayer attacker && attacker.isAlive()) {
            aiComponent.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, attacker.getRuntimeId());
        }

        return true;
    }

    /**
     * Kup oldugunde onu iki kucuk parcaya boler.
     *
     * <p>En kucuk boyut bolunmez, aksi halde kuplerin sonu gelmezdi.</p>
     */
    @EventHandler
    protected void onDie(CEntityDieEvent event) {
        var size = cubeBaseComponent.getCubeSize();
        if (size <= EntityCubeBaseComponentImpl.SIZE_SMALL) {
            return;
        }

        var type = selfType.get();
        var dimension = thisEntity.getDimension();
        if (type == null || dimension == null) {
            return;
        }

        var location = thisEntity.getLocation();
        for (int i = 0; i < SPLIT_COUNT; i++) {
            // Parcalari birbirinin icine dogurma; yoksa carpisma cozumu onlari firlatir.
            var offset = (i == 0 ? -SPLIT_SPREAD : SPLIT_SPREAD);
            var spawnLoc = new Location3d(
                    location.x() + offset, location.y(), location.z() + offset, dimension);

            var child = type.createEntity(EntityInitInfo.builder().loc(spawnLoc).build());
            if (child instanceof EntityCubeBaseComponent childCube) {
                childCube.setCubeSize(size / 2);
            }
            dimension.getEntityManager().addEntity(child);
        }
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drop = dropSupplier.get();
        if (drop == null) {
            return List.of();
        }

        var size = cubeBaseComponent.getCubeSize();
        if (dropOnlyWhenSmallest && size > EntityCubeBaseComponentImpl.SIZE_SMALL) {
            // Balcik yalnizca en kucuk parcasi olurken top birakir; buyugu bolunerek "kaybolur".
            return List.of();
        }

        var drops = new ArrayList<ItemStack>();
        drops.add(drop.createItemStack(1 + lootingLevel));
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return cubeBaseComponent.getCubeSize();
    }
}
