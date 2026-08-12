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
 * Sabit bir silahla dogan insansi moblar icin temel bilesen — yayiyla iskelet, arbaletiyle
 * pillager, baltasiyla vindicator.
 *
 * <p>Silah yalnizca el hala bossa verilir. El gozu {@link EntityHumanLikeContainerHolderComponentImpl}
 * tarafindan NBT'ye {@code Mainhand} olarak yazildigi icin diskten yuklenen bir mob silahini
 * zaten tasiyor olur — ustelik bir eklenti ya da oyuncu degistirdiyse baska bir silahi. Her
 * yuklemede yeniden silahlandirmak bunu bozardi.</p>
 *
 * <p>Bu bilesen ayrica istemcinin silahi canlandirmak icin ihtiyac duydugu savas durumunu da
 * tasir. Elde esya tutmak yetmiyor: bir illager saldirgan hale gelene kadar silahini indirik
 * tutar, yay ve arbalet de yalnizca mob uygun {@link WeaponStance} icindeyken cekme
 * animasyonunu oynatir. Ikisi de varlik durumu yayiniyla, ve yalnizca gercekten degistiklerinde
 * gonderilir.</p>
 *
 * <p>Ayni ofke takibini istemesine ragmen {@link EntityAngerableBaseComponentImpl}'i
 * genisletemez, cunku {@link EntityHumanLikeBaseComponentImpl}'in dogma mantigina ihtiyaci var —
 * eldeki esyayi ve zirhi yeni bir izleyiciye gonderen sey odur. Ortak kisim
 * {@link EntityAngerableBaseComponentImpl#isHunting} uzerinden yeniden kullaniliyor.</p>
 */
public class EntityArmedBaseComponentImpl extends EntityHumanLikeBaseComponentImpl implements EntityWeaponStanceComponent {

    @Dependency
    protected EntityContainerHolderComponent containerHolderComponent;

    protected final Supplier<ItemType<?>> weaponSupplier;
    protected final AABBdc baseAABB;

    protected WeaponStance weaponStance = WeaponStance.IDLE;
    protected boolean aggressive;

    /**
     * @param initInfo varlik baslatma bilgisi
     * @param weaponSupplier dogarken verilecek silah turunu saglar; gec cozulur cunku
     *                       {@code ItemTypes} alanlari turler kaydedilirken hala null'dur
     * @param width carpisma kutusu genisligi (blok)
     * @param height carpisma kutusu yuksekligi (blok)
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
