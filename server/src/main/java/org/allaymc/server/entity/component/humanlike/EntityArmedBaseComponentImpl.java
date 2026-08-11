package org.allaymc.server.entity.component.humanlike;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.data.WeaponStance;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.EntityAngerableBaseComponentImpl;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.function.Supplier;

/**
 * Base component for human-like mobs that spawn holding a fixed weapon — skeleton with its bow,
 * pillager with its crossbow, vindicator with its axe.
 *
 * <p>The weapon is only handed out when the hand is still empty. The hand slot is written to NBT as
 * {@code Mainhand} by {@link EntityHumanLikeContainerHolderComponentImpl}, so a mob loaded back
 * from disk already carries its weapon — and possibly a different one, if a plugin or a player
 * changed it. Re-arming it on every load would undo that.</p>
 *
 * <p>This component also carries the combat state the client needs to animate the weapon. Holding
 * an item is not enough: an illager keeps its weapon lowered until it is aggressive, and a bow or
 * crossbow only plays its draw animation while the mob is in the matching {@link WeaponStance}.
 * Both are pushed with an entity state broadcast, and only when they actually change.</p>
 *
 * <p>It cannot extend {@link EntityAngerableBaseComponentImpl} even though it wants the same
 * aggression tracking, because it needs {@link EntityHumanLikeBaseComponentImpl}'s spawn logic —
 * that is what sends the held item and armor to a new viewer in the first place. The shared part is
 * reused through {@link EntityAngerableBaseComponentImpl#isHunting}.</p>
 */
public class EntityArmedBaseComponentImpl extends EntityHumanLikeBaseComponentImpl implements EntityWeaponStanceComponent {

    @Dependency
    protected EntityContainerHolderComponent containerHolderComponent;

    protected final Supplier<ItemType<?>> weaponSupplier;
    protected final AABBdc baseAABB;

    protected WeaponStance weaponStance = WeaponStance.IDLE;
    protected boolean aggressive;

    /**
     * @param initInfo the entity init info.
     * @param weaponSupplier supplies the weapon type to spawn with; resolved lazily because
     *                       {@code ItemTypes} fields are still null while types are registered.
     * @param width the hitbox width in blocks.
     * @param height the hitbox height in blocks.
     */
    public EntityArmedBaseComponentImpl(EntityInitInfo initInfo, Supplier<ItemType<?>> weaponSupplier,
                                        double width, double height) {
        super(initInfo);
        this.weaponSupplier = weaponSupplier;
        var halfWidth = width / 2;
        this.baseAABB = new AABBd(-halfWidth, 0.0, -halfWidth, halfWidth, height, halfWidth);
    }

    @Override
    public AABBdc getBaseAABB() {
        return baseAABB;
    }

    @Override
    public WeaponStance getWeaponStance() {
        return weaponStance;
    }

    @Override
    public void setWeaponStance(WeaponStance stance) {
        if (this.weaponStance == stance) {
            return;
        }

        this.weaponStance = stance;
        broadcastState();
    }

    @Override
    public boolean isAggressive() {
        return aggressive;
    }

    @Override
    public void setAggressive(boolean aggressive) {
        if (this.aggressive == aggressive) {
            return;
        }

        this.aggressive = aggressive;
        broadcastState();
    }

    @EventHandler
    protected void onLoadNBT(CEntityLoadNBTEvent event) {
        var handContainer = containerHolderComponent.getContainer(ContainerTypes.ENTITY_HAND);
        if (handContainer.getItemInHand().getItemType() != ItemTypes.AIR) {
            return;
        }

        var weapon = weaponSupplier.get();
        if (weapon != null) {
            handContainer.setItemInHand(weapon.createItemStack());
        }
    }

    @EventHandler
    protected void onAggressionTick(CEntityTickEvent event) {
        if (thisEntity instanceof EntityIntelligent intelligent) {
            setAggressive(EntityAngerableBaseComponentImpl.isHunting(intelligent));
        }
    }
}
