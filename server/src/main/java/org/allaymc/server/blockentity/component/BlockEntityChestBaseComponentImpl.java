package org.allaymc.server.blockentity.component;

import org.allaymc.api.block.action.SimpleBlockAction;
import org.allaymc.api.block.component.BlockOxidationComponent;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityPairableComponent;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.api.world.sound.Sound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.component.annotation.OnInitFinish;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author daoge_cmd
 */
public class BlockEntityChestBaseComponentImpl extends BlockEntityBaseComponentImpl {

    @Dependency
    private BlockEntityChestContainerHolderComponentImpl containerHolderComponent;
    @Dependency
    private BlockEntityPairableComponent pairableComponent;

    public BlockEntityChestBaseComponentImpl(BlockEntityInitInfo info) {
        super(info);
    }

    @OnInitFinish
    @Override
    public void onInitFinish(BlockEntityInitInfo initInfo) {
        super.onInitFinish(initInfo);

        var doubleChestContainer = containerHolderComponent.getDoubleChestContainerDirectly();
        doubleChestContainer.addOpenListener(viewer -> {
            if (doubleChestContainer.getViewers().size() == 1) {
                changeDoubleChestState(true);
            }
        });
        doubleChestContainer.addCloseListener(viewer -> {
            if (doubleChestContainer.getViewers().isEmpty()) {
                changeDoubleChestState(false);
            }
        });

        var container = containerHolderComponent.getContainer();
        container.addOpenListener(viewer -> {
            if (container.getViewers().size() == 1) {
                changeChestState(getPosition(), true);
            }
        });
        container.addCloseListener(viewer -> {
            if (container.getViewers().isEmpty()) {
                changeChestState(getPosition(), false);
            }
        });
    }

    protected void changeDoubleChestState(boolean open) {
        var pos = getPosition();
        sendChestAction(pos, open);

        var pair = pairableComponent.getPair();
        if (pair != null) {
            sendChestAction(pair.getPosition(), open);
        }

        playChestSound(pos, open);
    }

    protected static void changeChestState(Position3ic pos, boolean open) {
        sendChestAction(pos, open);
        playChestSound(pos, open);
    }

    protected static void sendChestAction(Position3ic pos, boolean open) {
        pos.dimension().addBlockAction(pos, open ? SimpleBlockAction.OPEN : SimpleBlockAction.CLOSE);
    }

    protected static void playChestSound(Position3ic pos, boolean open) {
        var dimension = pos.dimension();
        dimension.addSound(MathUtils.center(pos), resolveChestSound(dimension.getBlockState(pos), open));
    }

    protected static Sound resolveChestSound(BlockState blockState, boolean open) {
        if (!(blockState.getBehavior() instanceof BlockOxidationComponent oxidationComponent)) {
            return open ? SimpleSound.CHEST_OPEN : SimpleSound.CHEST_CLOSE;
        }

        var soundName = switch (oxidationComponent.getOxidationLevel()) {
            case WEATHERED -> open ? SoundNames.BLOCK_COPPER_CHEST_WEATHERED_OPEN : SoundNames.BLOCK_COPPER_CHEST_WEATHERED_CLOSED;
            case OXIDIZED -> open ? SoundNames.BLOCK_COPPER_CHEST_OXIDIZED_OPEN : SoundNames.BLOCK_COPPER_CHEST_OXIDIZED_CLOSED;
            default -> open ? SoundNames.BLOCK_COPPER_CHEST_OPEN : SoundNames.BLOCK_COPPER_CHEST_CLOSED;
        };
        return new CustomSound(soundName, 1, 0.9f + ThreadLocalRandom.current().nextInt(101) / 1000f);
    }
}
