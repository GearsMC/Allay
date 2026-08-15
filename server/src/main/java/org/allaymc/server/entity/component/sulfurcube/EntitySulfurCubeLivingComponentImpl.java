package org.allaymc.server.entity.component.sulfurcube;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.eventbus.event.entity.EntityExplodeEvent;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.explosion.Explosion;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.EntityLivingComponentImpl;
import org.allaymc.server.entity.component.event.CEntityDieEvent;
import org.allaymc.server.entity.component.event.CEntitySulfurCubeChangeEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.List;
import java.util.Set;

/**
 * Sulfur kupu icin canli varlik bileseni: can, hasar bagisikligi, bolunme ve patlama.
 *
 * <p>Kup blok tasirken neredeyse hicbir seyden zarar gormez — bu, onu balcik ve magma kupunden
 * ayiran ikinci buyuk fark. Blogu makasla alinmadan ya da baska bir yolla cikarilmadan
 * oldurulemez; tek istisnasi kendi patlamasi.</p>
 */
public class EntitySulfurCubeLivingComponentImpl extends EntityLivingComponentImpl {

    /** Buyuk ve kucuk kupun cani. */
    protected static final float LARGE_HEALTH = 8;
    protected static final float SMALL_HEALTH = 4;

    /** Buyuk bir kup oldugunde yerine dogan kucuk kup sayisi. */
    protected static final int SPLIT_COUNT = 2;

    /** Parcalarin birbirinden ayrilma mesafesi (blok). */
    protected static final double SPLIT_SPREAD = 0.4;

    /** Patlamanin yaricapi; TNT'den daha kucuk. */
    protected static final float EXPLOSION_SIZE = 3;

    /**
     * Blok tasiyan bir kupun savusturdugu hasar turleri. Wiki'nin saydigi liste: yakin dovus,
     * mermi, dusen blok, patlama, dusme, donma, kaktus ve tatli meyve temasi, zehir, magma.
     */
    protected static final Set<DamageType> ABSORBED_BLOCK_IMMUNITIES = Set.of(
            DamageType.ENTITY_ATTACK, DamageType.PROJECTILE, DamageType.FALLING_BLOCK,
            DamageType.ENTITY_EXPLOSION, DamageType.BLOCK_EXPLOSION, DamageType.FALL,
            DamageType.FREEZING, DamageType.CONTACT, DamageType.MAGIC, DamageType.MAGMA);

    @Dependency
    protected EntitySulfurCubeBaseComponent cubeBaseComponent;

    /** En zayif vurusta bile kupun savrulmasini saglayan taban firlatma gucu. */
    protected static final double BASE_LAUNCH = 0.4;

    /** Yutulan her bir hasar puani basina eklenen firlatma gucu. */
    protected static final double LAUNCH_PER_DAMAGE = 0.08;

    /**
     * Dikey firlatmanin yatay firlatmaya orani.
     *
     * <p>Resmi tanimda kupu itmenin dikey carpani {@code vertical_kick_multiplier: 0.3}. Ikisine
     * ayni gucu vermek kupu yere paralel savurmak yerine neredeyse dik yukari atiyordu; blok
     * tasiyan kupun "direkt yukari firlamasi" bundandi.</p>
     */
    protected static final double LAUNCH_VERTICAL_RATIO = 0.3;

    /** Sicak kisilikte iki hasar arasindaki tick sayisi. */
    protected static final int HOT_DAMAGE_INTERVAL = 10;

    /** Sicak kisilikte her seferinde verilen hasar. */
    protected static final float HOT_DAMAGE = 1;

    /** Patlama sirasinda kendi hasarindan kacinmamak icin acilan kapi. */
    protected boolean exploding;

    protected int hotDamageCooldown;

    public EntitySulfurCubeLivingComponentImpl() {
        setMaxHealth(LARGE_HEALTH);
    }

    @Override
    public boolean hasFallDamage() {
        // Kup zaten bir top gibi sekiyor; dusme hasari o hissi bozardi.
        return false;
    }

    @Override
    public boolean canBeAttacked(DamageContainer damage) {
        if (!super.canBeAttacked(damage)) {
            return false;
        }

        // Fitili yanan kupe dokunulamaz; patlamasini beklemekten baska care yok.
        return !cubeBaseComponent.isIgnited() || exploding;
    }

    /**
     * Blok tasiyan kupa vurulunca hasar gecmez, ama kup firlar.
     *
     * <p>Bunu yapmanin iki yolu var ve ikisi de yanlis sonuc veriyordu. Vurusu
     * {@link #canBeAttacked} icinde reddetmek geri tepmeyi de iptal ediyor, kup oldugu yerde
     * kaliyordu. Vurusu normal isletip yalnizca hasari sifirlamak ise kupu firlatiyor ama vurus
     * "basarili" sayildigi icin kup kirmizi yanip vurus sesi cikariyordu — oysa hicbir sey
     * hissetmemis olmasi gerek.</p>
     *
     * <p>Dogrusu ucuncu yol: vurus hic islemez, geri tepme elle uygulanir. Boylece ne hasar ne
     * kirmizi yanip sonme ne de vurus sesi olur; yalnizca kup savrulur. Wiki firlama gucunun
     * "vurusun verecegi hasar kadar" oldugunu soyluyor, o yuzden guc ham hasara gore olcekleniyor.</p>
     */
    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        // Ates ve patlama, hasar gecsin ya da gecmesin fitili tutusturur; bu yuzden bagisiklik
        // kontrolunden once bakiliyor. TNT tasiyan bir kup patlamaya zaten bagisik, ama zincirleme
        // patlamanin calismasi tam da buna bagli.
        tryIgniteFrom(damage);

        if (resistsWithBlock(damage)) {
            launch(damage);
            return false;
        }

        return super.attack(damage, ignoreCoolDown);
    }

    /**
     * Ates ve patlama TNT tasiyan kupu ateslar. Zincirleme patlamanin fitili kisa, boylece bir kup
     * yigini pesi sira patliyor.
     */
    protected void tryIgniteFrom(DamageContainer damage) {
        if (cubeBaseComponent.isIgnited()) {
            return;
        }

        var type = damage.getDamageType();
        if (type == DamageType.ENTITY_EXPLOSION || type == DamageType.BLOCK_EXPLOSION) {
            cubeBaseComponent.ignite(EntitySulfurCubeBaseComponentImpl.randomChainFuse());
        } else if (type == DamageType.FIRE || type == DamageType.FIRE_TICK || type == DamageType.LAVA) {
            cubeBaseComponent.ignite(EntitySulfurCubeBaseComponentImpl.MANUAL_FUSE_TICKS);
        }
    }

    /**
     * @return kupun tasidigi blok sayesinde bu hasari tumden savusturup savusturmadigi
     */
    protected boolean resistsWithBlock(DamageContainer damage) {
        return !exploding
               && cubeBaseComponent.getAbsorbedBlock() != null
               && ABSORBED_BLOCK_IMMUNITIES.contains(damage.getDamageType());
    }

    /**
     * Savusturulan vurusun kupu firlatmasi. Hasar gecmedigi icin geri tepmeyi motor kendisi
     * uygulamiyor; buradan elle veriliyor.
     */
    protected void launch(DamageContainer damage) {
        if (!(thisEntity instanceof EntityPhysicsComponent physics)) {
            return;
        }

        var source = damage.getKnockbackSource();
        if (source == null && damage.getAttacker() instanceof Entity attacker) {
            source = attacker.getLocation();
        }
        if (source == null) {
            return;
        }

        // Kisiligin negatif geri tepme direnci "daha uzaga firlat" demek; resmi tanimda lastik top
        // ve yapiskan icin -2.0, yani kup normalin uc kati savruluyor.
        var archetype = cubeBaseComponent.getArchetype();
        var multiplier = archetype == null ? 1 : archetype.getLaunchMultiplier();
        var force = (BASE_LAUNCH + damage.getSourceDamage() * LAUNCH_PER_DAMAGE) * multiplier;
        physics.knockback(source, force, force * LAUNCH_VERTICAL_RATIO);
    }

    /**
     * Sicak kisilikteki kup, degdigi varliklari magma blogu gibi yakar.
     */
    @EventHandler
    protected void onHotTick(CEntityTickEvent event) {
        if (!isHot() || !thisEntity.isAlive()) {
            return;
        }

        if (++hotDamageCooldown < HOT_DAMAGE_INTERVAL) {
            return;
        }

        hotDamageCooldown = 0;
        for (var other : thisEntity.getDimension().getEntityManager()
                .getPhysicsService().computeCollidingEntities(thisEntity.getOffsetAABB())) {
            if (other == thisEntity || !(other instanceof EntityLiving living)) {
                continue;
            }

            living.attack(DamageContainer.magma(HOT_DAMAGE));
        }
    }

    /**
     * Boyut degistiginde cani ona gore ayarlar.
     */
    @EventHandler
    protected void onCubeChange(CEntitySulfurCubeChangeEvent event) {
        var target = event.isLarge() ? LARGE_HEALTH : SMALL_HEALTH;
        if (getMaxHealth() == target) {
            return;
        }

        setMaxHealth(target);
        setHealth(target);
    }

    /**
     * Fitil bittiginde kupu patlatir.
     *
     * <p>Patlamayla olen kup bolunmez; wiki bunu ozellikle belirtiyor. Bolunmeyi engellemenin yolu
     * kupu once kaldirip sonra patlatmak: olum olayi tetiklendiginde varlik dunyada olmadigi icin
     * parcalar da dogmuyor.</p>
     */
    @EventHandler
    protected void onExplode(CEntitySulfurCubeExplodeEvent event) {
        var dimension = thisEntity.getDimension();
        if (dimension == null) {
            return;
        }

        exploding = true;
        var location = thisEntity.getLocation();
        thisEntity.remove();

        var explosion = new Explosion(EXPLOSION_SIZE);
        explosion.setEntity(thisEntity);
        var explodeEvent = new EntityExplodeEvent(thisEntity, explosion);
        if (explodeEvent.call()) {
            explosion.explode(dimension, location.x(), location.y(), location.z());
        }
    }

    /**
     * Buyuk kup oldugunde iki kucuk kupe bolunur ve tasidigi blogu birakir.
     */
    @EventHandler
    protected void onDie(CEntityDieEvent event) {
        if (cubeBaseComponent instanceof EntitySulfurCubeBaseComponentImpl impl) {
            impl.dropAbsorbedBlock();
        }

        if (!cubeBaseComponent.isLarge() || exploding) {
            return;
        }

        var dimension = thisEntity.getDimension();
        if (dimension == null || EntityTypes.SULFUR_CUBE == null) {
            return;
        }

        var location = thisEntity.getLocation();
        for (int i = 0; i < SPLIT_COUNT; i++) {
            // Parcalari birbirinin icine dogurma; yoksa carpisma cozumu onlari firlatir.
            var offset = i == 0 ? -SPLIT_SPREAD : SPLIT_SPREAD;
            var spawnLoc = new Location3d(
                    location.x() + offset, location.y(), location.z() + offset, dimension);

            var child = EntityTypes.SULFUR_CUBE.createEntity(
                    EntityInitInfo.builder().loc(spawnLoc).build());
            if (child instanceof EntitySulfurCubeBaseComponent childCube) {
                childCube.setLarge(false);
            }
            dimension.getEntityManager().addEntity(child);
        }
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        // Tasidigi blok olum olayinda birakiliyor; loot tablosunda ayrica dusurulmemeli.
        return List.of();
    }

    @Override
    public int getDropXpAmount() {
        // Yalnizca buyuk kup deneyim birakir.
        return cubeBaseComponent.isLarge() ? 1 + (int) (Math.random() * 2) : 0;
    }

    /**
     * @return kupun sicak kisilikte olup olmadigi; yakinindakilere magma gibi hasar verir
     */
    public boolean isHot() {
        return cubeBaseComponent.getArchetype() == SulfurCubeArchetype.HOT;
    }
}
