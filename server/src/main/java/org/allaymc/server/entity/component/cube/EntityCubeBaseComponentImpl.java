package org.allaymc.server.entity.component.cube;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityCubeBaseComponent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.component.event.CEntityCubeSizeChangeEvent;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Kup moblarinin — balcik ve magma kupu — temel davranisi: boyut ve ona bagli carpisma kutusu.
 *
 * <p>Boyut varligin butun olculerini belirledigi icin burada tutuluyor ve NBT'ye yaziliyor:
 * yeniden baslatmadan sonra buyuk bir balcigin kucucuk dogmasi olmaz. Carpisma kutusu boyutla
 * birlikte buyudugu icin {@link #getBaseAABB()} sabit degil, hesaplanan bir deger dondurur.</p>
 */
public class EntityCubeBaseComponentImpl extends EntityBaseComponentImpl implements EntityCubeBaseComponent {

    /** Vanilla'daki uc boyut. */
    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_LARGE = 4;

    protected static final String TAG_SIZE = "Size";

    /** Boyut basina carpisma kutusu kenari (blok); vanilla ile ayni. */
    protected static final double EDGE_PER_SIZE = 0.51;

    protected int cubeSize;

    public EntityCubeBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
        // Dogal dogumda uc boyuttan biri; NBT'den yuklenirse asagida uzerine yazilir.
        this.cubeSize = switch (ThreadLocalRandom.current().nextInt(3)) {
            case 0 -> SIZE_SMALL;
            case 1 -> SIZE_MEDIUM;
            default -> SIZE_LARGE;
        };
    }

    @Override
    public int getCubeSize() {
        return cubeSize;
    }

    @Override
    public void setCubeSize(int size) {
        if (this.cubeSize == size) {
            return;
        }

        this.cubeSize = size;
        applySize();
    }

    /**
     * Yeni boyutu hem istemciye hem de boyuta bagli diger bilesenlere duyurur.
     */
    protected void applySize() {
        // Olcek istemcinin kupu ne kadar buyuk cizecegini belirler ve metadata ile gidiyor.
        setScale(cubeSize);
        manager.callEvent(new CEntityCubeSizeChangeEvent(cubeSize));
        broadcastState();
    }

    @Override
    public AABBdc getBaseAABB() {
        var half = cubeSize * EDGE_PER_SIZE / 2;
        return new AABBd(-half, 0.0, -half, half, cubeSize * EDGE_PER_SIZE, half);
    }

    /**
     * Kaydedilmis boyutu geri okur ve boyutu duyurur.
     *
     * <p>Duyuru burada yapiliyor cunku bu, boyutun kesinlestigi ilk an: kurucu calisirken bilesen
     * yoneticisi henuz enjekte edilmemis oluyor, dolayisiyla oradan olay tetiklenemiyor. Bu
     * isleyici hem diskten yuklenen hem de yeni dogan kupler icin calisir.</p>
     */
    @EventHandler
    protected void onLoadNBT(CEntityLoadNBTEvent event) {
        event.getNbt().listenForInt(TAG_SIZE, size -> this.cubeSize = size);
        applySize();
    }

    @EventHandler
    protected void onSaveNBT(CEntitySaveNBTEvent event) {
        event.getNbt().putInt(TAG_SIZE, this.cubeSize);
    }
}
