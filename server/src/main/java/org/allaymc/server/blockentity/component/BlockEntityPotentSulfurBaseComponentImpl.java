package org.allaymc.server.blockentity.component;

import lombok.Getter;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityPotentSulfurBaseComponent;
import org.cloudburstmc.nbt.NbtMap;

public class BlockEntityPotentSulfurBaseComponentImpl extends BlockEntityBaseComponentImpl implements BlockEntityPotentSulfurBaseComponent {

    protected static final String TAG_COUNTDOWN = "countdown";

    @Getter
    protected int countdown;
    protected boolean loadUpdateScheduled;

    public BlockEntityPotentSulfurBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    public static int computeDormantValue(int x, int y, int z) {
        var hash = ((x * 3129871L) ^ (z * 116129781L) ^ (y * 8675309L)) & 0x7fffffffL;
        return 15 + (int) (hash % 16);
    }

    public static int computeEruptionValue(int x, int y, int z) {
        var hash = ((((long) x << 16) ^ ((long) z << 8) ^ y) * 73244475L) & 0x7fffffffL;
        return 1 + (int) (hash % 2);
    }

    public static int computeDormantDurationSeconds(int waterHeight, int x, int y, int z) {
        return 10 * (waterHeight - 1) + computeDormantValue(x, y, z);
    }

    public static int computeEruptionDurationSeconds(int waterHeight, int x, int y, int z) {
        return (waterHeight - 1) + computeEruptionValue(x, y, z);
    }

    @Override
    public void setCountdown(int countdown) {
        this.countdown = Math.max(0, countdown);
    }

    @Override
    public void tick(long currentTick) {
        super.tick(currentTick);
        if (loadUpdateScheduled) {
            return;
        }

        loadUpdateScheduled = true;
        var blockUpdateManager = position.dimension().getBlockUpdateManager();
        if (!blockUpdateManager.hasScheduledBlockUpdate(position)) {
            blockUpdateManager.scheduleBlockUpdateInDelay(position, 1);
        }
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForInt(TAG_COUNTDOWN, value -> countdown = value);
    }

    @Override
    public NbtMap saveNBT() {
        var nbt = super.saveNBT();
        if (countdown <= 0) {
            return nbt;
        }

        return nbt.toBuilder()
                .putInt(TAG_COUNTDOWN, countdown)
                .build();
    }
}
