package org.allaymc.server.entity.component.sulfurcube;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.world.WorldViewer;
import org.allaymc.api.world.particle.CustomParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySulfurCubeChangeEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sulfur kupunun temel davranisi: boyut, emilen blok, buyume ve fitil.
 *
 * <p>Kupun butun ayirt edici ozellikleri burada bulusuyor. Emilen blok hem gorunumu (istemciye
 * kupun icinde cizilir) hem de fizigi belirliyor, boyut hem cani hem carpisma kutusunu, fitil ise
 * kupu gecici olarak dokunulmaz kiliyor.</p>
 */
public class EntitySulfurCubeBaseComponentImpl extends EntityBaseComponentImpl implements EntitySulfurCubeBaseComponent {

    /** Kucuk bir kupun buyumesi icin gereken sure; vanilla'da yirmi dakika. */
    public static final int GROWTH_TICKS = 20 * 60 * 20;

    /** Elle ateslendiginde fitil suresi. */
    public static final int MANUAL_FUSE_TICKS = 120;

    /**
     * Baska bir patlamayla ateslendiginde fitilin alt ve ust siniri.
     *
     * <p>Resmi davranis paketi bunu saniye cinsinden veriyor: {@code 0.75} ile {@code 3.0} arasi,
     * yani 15 ile 60 tick.</p>
     */
    public static final int CHAIN_FUSE_MIN_TICKS = 15;
    public static final int CHAIN_FUSE_MAX_TICKS = 60;

    /** Slime topuyla beslendiginde buyume sayacina eklenen sure. */
    protected static final int SLIMEBALL_GROWTH_BOOST = 20 * 60 * 5;

    /**
     * Blogunu kaybettikten sonra kupun yerden esya alamadigi sure.
     *
     * <p>Resmi davranis paketinde bu, blok cikarilinca eklenen bes saniyelik bir zamanlayici
     * ({@code minecraft:timer}, {@code "time": 5}); suresi dolunca kupun esya toplama davranisi geri
     * geliyor.</p>
     */
    public static final int PICKUP_TIMEOUT_TICKS = 20 * 5;

    protected static final double LARGE_EDGE = 0.98;
    protected static final double SMALL_EDGE = 0.49;

    protected static final String TAG_LARGE = "Large";
    protected static final String TAG_ABSORBED = "AbsorbedBlockStateHash";
    protected static final String TAG_GROWTH = "GrowthTicks";
    protected static final String TAG_GROWTH_BLOCKED = "GrowthBlocked";

    /** Ziplarken ve inerken sacilan parcacik. */
    protected static final String JUMP_PARTICLE = "minecraft:sulfur_cube_goo";

    /** Ziplama sesi. */
    protected static final String JUMP_SOUND = "mob.sulfur_cube.small.jump";

    /** Altinda kupun "yere kondu" sayildigi yatay hiz. */
    protected static final double MIN_HORIZONTAL_SPEED = 0.05;

    protected boolean large = true;
    protected boolean jumping;
    protected int jumpTicks;
    protected BlockState absorbedBlock;
    protected SulfurCubeArchetype archetype;
    protected int growthTicks;
    protected boolean growthBlocked;
    protected int fuseTicks = -1;
    protected int pickupCooldown;

    public EntitySulfurCubeBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public boolean isLarge() {
        return large;
    }

    @Override
    public void setLarge(boolean large) {
        if (this.large == large) {
            return;
        }

        this.large = large;
        if (!large) {
            // Kuculen bir kup tasidigi blogu birakmak zorunda; kucuk kupler blok tasiyamaz.
            this.absorbedBlock = null;
            this.archetype = null;
        }
        notifyChange();
    }

    @Override
    public BlockState getAbsorbedBlock() {
        return absorbedBlock;
    }

    @Override
    public void setAbsorbedBlock(BlockState blockState) {
        // Kucuk kupler blok ememez; wiki bunu acikca soyluyor.
        if (!large && blockState != null) {
            return;
        }

        // Blogunu kaybeden kup bir sure hicbir sey alamaz. Bu kural resmi tanimda ayri bir zamanlayici
        // olarak duruyor ve olmadiginda kup makasla cikarilan blogu daha yere dusmeden geri yutuyor:
        // esyayi ayni anda hem oyuncu aliyor hem kup yutuyordu, yani blok kopyalaniyordu.
        if (absorbedBlock != null && blockState == null) {
            pickupCooldown = PICKUP_TIMEOUT_TICKS;
        }

        this.absorbedBlock = blockState;
        this.archetype = blockState == null ? null : SulfurCubeArchetypes.of(blockState.getBlockType());
        notifyChange();
    }

    @Override
    public boolean isPickupOnCooldown() {
        return pickupCooldown > 0;
    }

    @Override
    public SulfurCubeArchetype getArchetype() {
        return archetype;
    }

    @Override
    public boolean isIgnited() {
        return fuseTicks >= 0;
    }

    @Override
    public void ignite(int fuseTicks) {
        if (archetype != SulfurCubeArchetype.EXPLOSIVE || isIgnited()) {
            return;
        }

        this.fuseTicks = fuseTicks;
        // Dunyaya konmamis bir kup de ateslenebilir (ornegin dogurulmadan hazirlanirken);
        // sesi yalnizca dinleyecek biri varsa cal.
        var dimension = getDimension();
        if (dimension != null) {
            dimension.addSound(getLocation(), SimpleSound.IGNITE);
        }
        notifyChange();
    }

    /**
     * @return fitilin patlamaya kac tick kaldigi; yanmiyorsa negatif
     */
    public int getFuseTicks() {
        return fuseTicks;
    }

    /**
     * Buyume sayacini slime topuyla ileri alir.
     */
    public void boostGrowth() {
        growthTicks += SLIMEBALL_GROWTH_BOOST;
    }

    /**
     * Kupun bir daha buyumesini engeller; altin karahindiba boyle calisiyor.
     */
    public void blockGrowth() {
        growthBlocked = true;
    }

    /**
     * @return buyumesinin engellenip engellenmedigi
     */
    public boolean isGrowthBlocked() {
        return growthBlocked;
    }

    @Override
    public AABBdc getBaseAABB() {
        var edge = large ? LARGE_EDGE : SMALL_EDGE;
        var half = edge / 2;
        return new AABBd(-half, 0.0, -half, half, edge, half);
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null) {
            return false;
        }

        var itemType = itemStack.getItemType();

        // Yanan bir kupe hicbir sey yapilamaz; ne blogu alinir ne kovaya konur.
        if (isIgnited()) {
            return false;
        }

        // Resmi tanim makasi yalnizca comelmiyorken kabul ediyor.
        if (itemType == ItemTypes.SHEARS && absorbedBlock != null && !player.isSneaking()) {
            dropAbsorbedBlock();
            setAbsorbedBlock(null);
            return true;
        }

        // Cakmak ya da ates topu TNT tasiyan kupu ateslar.
        if ((itemType == ItemTypes.FLINT_AND_STEEL || itemType == ItemTypes.FIRE_CHARGE)
            && archetype == SulfurCubeArchetype.EXPLOSIVE) {
            ignite(MANUAL_FUSE_TICKS);
            return true;
        }

        // Bos kova buyuk kupu kovaya alir. Kovalanan kup dunyadan cikar, dolayisiyla mob
        // sinirina sayilmaz ve kaybolmaz.
        if (itemType == ItemTypes.BUCKET && large) {
            player.tryConsumeItemInHand();
            player.tryAddItem(ItemTypes.SULFUR_CUBE_BUCKET.createItemStack());
            thisEntity.remove();
            return true;
        }

        if (itemType == ItemTypes.SLIME_BALL && !large) {
            boostGrowth();
            player.tryConsumeItemInHand();
            return true;
        }

        if (itemType == ItemTypes.GOLDEN_DANDELION && !large) {
            blockGrowth();
            player.tryConsumeItemInHand();
            return true;
        }

        // Elindeki blogu uzatan oyuncudan blogu al.
        if (large && absorbedBlock == null) {
            var blockType = itemType.getBlockType();
            if (blockType != null && SulfurCubeArchetypes.isAbsorbable(blockType)) {
                setAbsorbedBlock(blockType.getDefaultState());
                player.tryConsumeItemInHand();
                playAbsorbSound();
                return true;
            }
        }

        return false;
    }

    /**
     * Blok yutma sesi. Kupun blogu nereden aldigi onemli degil — oyuncunun elinden de olsa yerden
     * de olsa ayni sesi cikarmali.
     */
    public void playAbsorbSound() {
        var dimension = getDimension();
        if (dimension != null) {
            dimension.addSound(getLocation(), SimpleSound.SPONGE_ABSORB);
        }
    }

    /**
     * Emilen blogu yere birakir. Kup oldugunde ve makasla alindiginda ayni yol kullanilir.
     */
    public void dropAbsorbedBlock() {
        var dimension = getDimension();
        if (absorbedBlock == null || dimension == null) {
            return;
        }

        var itemType = absorbedBlock.getBlockType().getItemType();
        if (itemType != null) {
            dimension.dropItem(itemType.createItemStack(), getLocation());
        }
    }

    @Override
    public boolean isJumping() {
        return jumping;
    }

    @Override
    public int getJumpDurationTicks() {
        return jumpTicks;
    }

    @Override
    public void startJump(int durationTicks) {
        jumping = true;
        jumpTicks = durationTicks;
        thisEntity.applyAction(SimpleEntityAction.JUMP);
        spawnGoo();
        var dimension = getDimension();
        if (dimension != null) {
            dimension.addSound(getLocation(), new CustomSound(JUMP_SOUND, 0.7f, 0.5f));
        }
        broadcastState();
    }

    /**
     * Ziplama animasyonunu geri sayar.
     *
     * <p>Animasyon iki sekilde biter: suresi dolar, ya da kup yere inip yatay hizini kaybeder.
     * Ikincisi olmadan kisa bir sicramada bile kup havadaymis gibi durmaya devam ederdi.</p>
     */
    protected void tickJump() {
        if (!jumping) {
            return;
        }

        jumpTicks = Math.max(0, jumpTicks - 1);
        if (jumpTicks <= 0) {
            endJump(false);
            return;
        }

        if (thisEntity instanceof EntityPhysicsComponent physics && physics.isOnGround()) {
            var motion = physics.getMotion();
            var horizontalSpeed = Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z());
            if (horizontalSpeed <= MIN_HORIZONTAL_SPEED) {
                endJump(true);
                return;
            }
        }

        broadcastState();
    }

    protected void endJump(boolean landed) {
        jumping = false;
        jumpTicks = 0;
        if (landed) {
            spawnGoo();
        }
        broadcastState();
    }

    /**
     * Kupun ziplarken ve inerken sactigi parcaciklar.
     */
    protected void spawnGoo() {
        var dimension = getDimension();
        if (dimension != null) {
            dimension.addParticle(getLocation(), new CustomParticle(JUMP_PARTICLE));
        }
    }

    @EventHandler
    protected void onTick(CEntityTickEvent event) {
        tickFuse();
        tickGrowth();
        tickJump();
        if (pickupCooldown > 0) {
            pickupCooldown--;
        }
    }

    /**
     * Fitili tuketir. Patlamanin kendisi canli varlik bilesenine ait, cunku kupu oldurmesi ve
     * bolunmeyi engellemesi gerekiyor.
     */
    protected void tickFuse() {
        if (fuseTicks < 0) {
            return;
        }

        if (--fuseTicks > 0) {
            return;
        }

        fuseTicks = -1;
        manager.callEvent(CEntitySulfurCubeExplodeEvent.INSTANCE);
    }

    /**
     * Kucuk kupu yirmi dakika sonra buyutur. Altin karahindiba yemis bir kup hic buyumez.
     */
    protected void tickGrowth() {
        if (large || growthBlocked) {
            return;
        }

        if (++growthTicks >= GROWTH_TICKS) {
            growthTicks = 0;
            setLarge(true);
        }
    }

    /**
     * Boyut, blok ya da fitil degistiginde hem istemciyi hem de diger bilesenleri haberdar eder.
     *
     * <p>Kupun ici iki ayri sinyalden olusuyor ve <em>ikisi de</em> gerekli:</p>
     *
     * <ul>
     *   <li>{@code minecraft:sulfur_cube_archetype} property'si: istemcinin render denetleyicisi
     *   bu deger {@code none} degilse sulfur cekirdegini tamamen gizliyor.</li>
     *   <li>Elindeki esya: yutulan blok kupun <em>tuttugu esya</em> olarak ciziliyor ve resmi
     *   kaynak paketindeki {@code held_item_scale} ile buyutulup kupun icini dolduruyor.</li>
     * </ul>
     *
     * <p>Yalnizca property gonderilirse cekirdek gizlenir ama yerine bir sey konmaz; kup tam da
     * bu yuzden ne tasirsa tasisin bombos gorunur.</p>
     */
    protected void notifyChange() {
        setPropertyValue(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE,
                archetype == null ? SulfurCubeArchetype.NONE : archetype);
        manager.callEvent(new CEntitySulfurCubeChangeEvent(large, absorbedBlock, archetype));
        broadcastState();
        broadcastAbsorbedBlock();
    }

    /**
     * Yutulan blogu kupu goren herkesin elinde gunceller.
     */
    protected void broadcastAbsorbedBlock() {
        var item = toHandItem();
        forEachViewers(viewer -> viewer.viewEntityHandItem(thisEntity, item));
    }

    /**
     * @return kupun elinde gosterilecek esya; blok tasimayan kup icin bos el.
     */
    protected ItemStack toHandItem() {
        return absorbedBlock == null ? ItemAirStack.AIR_STACK : absorbedBlock.toItemStack();
    }

    /**
     * Kupu yeni goren birine tasidigi blogu da yollar.
     *
     * <p>Blok varligin metadata'sinda tasinmadigi icin dogurma paketiyle birlikte gitmiyor;
     * ayrica gonderilmezse kupu sonradan goren oyuncu icini bos gorur.</p>
     */
    @Override
    public void spawnTo(WorldViewer viewer) {
        if (getViewers().contains(viewer)) {
            return;
        }

        super.spawnTo(viewer);
        viewer.viewEntityHandItem(thisEntity, toHandItem());
    }

    @EventHandler
    protected void onLoadNBT(CEntityLoadNBTEvent event) {
        var nbt = event.getNbt();
        nbt.listenForBoolean(TAG_LARGE, value -> this.large = value);
        nbt.listenForInt(TAG_ABSORBED, hash -> this.absorbedBlock = Registries.BLOCK_STATE_PALETTE.get(hash));
        nbt.listenForInt(TAG_GROWTH, value -> this.growthTicks = value);
        nbt.listenForBoolean(TAG_GROWTH_BLOCKED, value -> this.growthBlocked = value);

        this.archetype = absorbedBlock == null ? null : SulfurCubeArchetypes.of(absorbedBlock.getBlockType());
        // Dogal dogumda buyuk kup cikar; kucukler yalnizca bolunmeden dogar.
        notifyChange();
    }

    @EventHandler
    protected void onSaveNBT(CEntitySaveNBTEvent event) {
        var nbt = event.getNbt();
        nbt.putBoolean(TAG_LARGE, large);
        nbt.putInt(TAG_GROWTH, growthTicks);
        nbt.putBoolean(TAG_GROWTH_BLOCKED, growthBlocked);
        if (absorbedBlock != null) {
            nbt.putInt(TAG_ABSORBED, absorbedBlock.blockStateHash());
        }
    }

    /**
     * @return zincirleme patlamalar icin rastgele kisa fitil suresi
     */
    public static int randomChainFuse() {
        return ThreadLocalRandom.current().nextInt(CHAIN_FUSE_MIN_TICKS, CHAIN_FUSE_MAX_TICKS + 1);
    }
}
