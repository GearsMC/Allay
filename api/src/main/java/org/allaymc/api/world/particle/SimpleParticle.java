package org.allaymc.api.world.particle;

/**
 * SimpleParticle contains all particle effects that do not require additional parameters.
 *
 * @author daoge_cmd
 */
public enum SimpleParticle implements Particle {
    /// BLOCK_FORCE_FIELD is a particle that shows up as a block that turns invisible.
    BLOCK_FORCE_FIELD,
    /// BONE_MEAL is a particle that shows up on bone meal usage.
    BONE_MEAL,
    /// EVAPORATE is a particle that shows up when a water block evaporates.
    EVAPORATE,
    /// WATER_DRIP is a particle that shows up when water drips from a block.
    WATER_DRIP,
    /// LAVA_DRIP is a particle that shows up when lava drips from a block.
    LAVA_DRIP,
    /// LAVA is a particle that shows up randomly above lava.
    LAVA,
    /// DUST_PLUME is a particle that shows up when an item is inserted into a decorated pot.
    DUST_PLUME,
    /// EXPLODE is a particle shown when sponge absorb water.
    EXPLODE,
    /// HUGE_EXPLOSION is a particle shown when TNT or a creeper explodes.
    HUGE_EXPLOSION,
    /// ENDERMAN_TELEPORT is a particle that shows up when an enderman teleports or a player uses ender pearl.
    ENDERMAN_TELEPORT,
    /// SNOWBALL_POOF is a particle shown when a snowball collides with something.
    SNOWBALL_POOF,
    /// ENTITY_FLAME is a particle shown when an entity is set on fire.
    ENTITY_FLAME,
    /// WHITE_SMOKE is a particle shown when the flame on an entity is extinguished by water.
    WHITE_SMOKE,
    /// FIREWORK_CONTRAIL is a particle shown at the location where the firework rocket flew across.
    FIREWORK_CONTRAIL,
    /// SMASH_ATTACK_GROUND_DUST is a particle that shows up when a mace smash attack hits the ground.
    SMASH_ATTACK_GROUND_DUST,
    /// WIND_EXPLOSION is a particle shown when a player wind charge explodes.
    WIND_EXPLOSION,
    /// BREEZE_WIND_EXPLOSION is a particle shown when a breeze wind charge explodes.
    BREEZE_WIND_EXPLOSION,
    /// WATER_WAKE is a particle shown when a fish is attracted to a fishing hook.
    WATER_WAKE,
    /// BUBBLE is a particle shown when a fish bites the fishing hook.
    BUBBLE,
    /// VILLAGER_HAPPY is a particle shown when a villager is pleased, and when bone meal is used on a crop.
    VILLAGER_HAPPY,
    /// END_ROD is a particle that trails from an end rod.
    END_ROD,
    /// CAMPFIRE_SMOKE is a particle that rises from a campfire.
    CAMPFIRE_SMOKE,
    /// CHERRY_LEAVES is a particle that falls from cherry leaves.
    CHERRY_LEAVES,
    /// PALE_OAK_LEAVES is a particle that falls from pale oak leaves.
    PALE_OAK_LEAVES,
    /// SONIC_EXPLOSION, sonik patlama parçacığıdır.
    SONIC_EXPLOSION,

    /// GearsMC fork: kizgin koylu parcacigi (PocketMine AngryVillagerParticle).
    VILLAGER_ANGRY,

    /// GearsMC fork: murekkep parcacigi (PocketMine InkParticle).
    INK,

    /// GearsMC fork: elder guardian laneti (jumpscare) efekti.
    ///
    /// PocketMine bunu `LevelEvent::GUARDIAN_CURSE` adiyla ham paket gondererek
    /// yapiyordu; Cloudburst'te ayni sayisal olayin adi
    /// `PARTICLE_SOUND_GUARDIAN_GHOST`. Ham paket gonderimi API'den kaldirildigi
    /// icin efekt buraya bir parcacik olarak eklendi: paketi hangi protokol
    /// surumune gore kuracagina sunucu karar verir.
    GUARDIAN_CURSE
}
