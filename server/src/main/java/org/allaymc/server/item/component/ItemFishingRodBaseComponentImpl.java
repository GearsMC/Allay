package org.allaymc.server.item.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.eventbus.event.player.PlayerStartFishEvent;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.component.annotation.ComponentObject;
import org.joml.Vector3d;

/**
 * Component implementation for fishing rod item.
 *
 * @author daoge_cmd
 */
public class ItemFishingRodBaseComponentImpl extends ItemBaseComponentImpl {

    /**
     * Durability cost when casting the fishing rod.
     */
    protected static final int DURABILITY_COST_CAST = 1;
    /**
     * Durability cost when catching an entity.
     */
    protected static final int DURABILITY_COST_ENTITY = 3;
    /**
     * Initial throwing force.
     *
     * <p>GearsMC fork: PocketMine sunucusundaki kanca {@code bakis * 1.2} ile firlatiliyor
     * ve her tikte iki kez hareket ediyordu (hem {@code Entity::onUpdate} hem kendi
     * {@code entityBaseTick}'i {@code move} cagiriyordu). Iki hareketin toplami
     * {@code v + (0.95v - 0.05)} oldugu icin tek hareketli karsiligi ilk tikte
     * {@code 1.95 * 1.2 = 2.34} kat hiz ve {@code 0.05} dusustur; sonraki tikler
     * {@link org.allaymc.server.entity.component.item.EntityFishingHookPhysicsComponentImpl}
     * icinde ayni seriyi surdurur. Vanilla deger 0.8 kancayi PM'e gore uc kat yavas
     * gosteriyordu.</p>
     */
    protected static final double THROW_FORCE = 2.34;
    /**
     * GearsMC fork: PM'deki ilk tikin yercekimi payi (bkz. {@link #THROW_FORCE}).
     */
    protected static final double THROW_INITIAL_DROP = 0.05;
    /**
     * GearsMC fork: kancanin goz hizasindan bakis yonunde ne kadar ileride dogacagi; PM
     * {@code getEyePos()->addVector($direction->multiply(0.6))}.
     */
    protected static final double SPAWN_FORWARD_OFFSET = 0.6;

    @ComponentObject
    protected ItemStack thisItemStack;

    public ItemFishingRodBaseComponentImpl(ItemStackInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public void rightClickItemInAir(EntityPlayer player) {
        if (player.isFishing()) {
            // Already fishing - reel in
            reelIn(player);
        } else {
            // Not fishing - cast
            cast(player);
        }
        player.applyAction(SimpleEntityAction.SWING_ARM);
    }

    /**
     * Casts the fishing line.
     *
     * @param player the player casting the line
     */
    protected void cast(EntityPlayer player) {
        var dimension = player.getDimension();
        var location = player.getLocation();

        // Calculate initial position and velocity
        // GearsMC fork: dogus noktasi ve hiz PM ile ayni (bkz. THROW_FORCE).
        var direction = MathUtils.getDirectionVector(location);
        var spawnPos = new Vector3d(
                location.x() + direction.x() * SPAWN_FORWARD_OFFSET,
                location.y() + player.getEyeHeight() + direction.y() * SPAWN_FORWARD_OFFSET,
                location.z() + direction.z() * SPAWN_FORWARD_OFFSET
        );
        var motion = direction.mul(THROW_FORCE, new Vector3d());
        motion.y -= THROW_INITIAL_DROP;

        // Create fishing hook entity
        var fishingHook = EntityTypes.FISHING_HOOK.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(spawnPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(motion)
                        .build()
        );

        // Set shooter and fishing rod reference
        fishingHook.setShooter(player);
        fishingHook.setFishingRod(thisItemStack);

        var event = new PlayerStartFishEvent(player, fishingHook);
        if (!event.call()) {
            return;
        }

        // Add entity to world
        dimension.getEntityManager().addEntity(fishingHook);

        // Link player to fishing hook
        player.setFishingHook(fishingHook);

        // Consume durability (only in survival/adventure)
        if (player.getGameMode() != GameMode.CREATIVE) {
            tryIncreaseDamage(DURABILITY_COST_CAST);
        }

        // Play throw sound
        dimension.addSound(location.x(), location.y(), location.z(), SimpleSound.ITEM_THROW);
    }

    /**
     * Reels in the fishing line.
     *
     * @param player the player reeling in
     */
    protected void reelIn(EntityPlayer player) {
        var fishingHook = player.getFishingHook();
        if (fishingHook == null) {
            return;
        }

        // Check if we caught an entity (for durability cost calculation)
        boolean caughtEntity = fishingHook.hasHookedEntity();

        // Reel line (handles loot dropping, entity pulling, etc.)
        fishingHook.reelLine();

        // Consume durability for catching entities (only in survival/adventure)
        if (caughtEntity && player.getGameMode() != GameMode.CREATIVE) {
            tryIncreaseDamage(DURABILITY_COST_ENTITY);
        }
    }
}
