package org.allaymc.server.blockentity.component;

import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityContainerHolderComponent;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.component.annotation.OnInitFinish;
import org.allaymc.server.container.impl.BarrelContainerImpl;

/**
 * @author daoge_cmd
 */
public class BlockEntityBarrelBaseComponentImpl extends BlockEntityBaseComponentImpl {
    @Dependency
    private BlockEntityContainerHolderComponent containerHolderComponent;

    public BlockEntityBarrelBaseComponentImpl(BlockEntityInitInfo info) {
        super(info);
    }

    @OnInitFinish
    @Override
    public void onInitFinish(BlockEntityInitInfo initInfo) {
        super.onInitFinish(initInfo);
        BarrelContainerImpl container = containerHolderComponent.getContainer();
        container.addOpenListener(viewer -> {
            if (container.getViewers().size() == 1) {
                changeBarrelState(true);
            }
        });
        container.addCloseListener(viewer -> {
            if (container.getViewers().isEmpty()) {
                changeBarrelState(false);
            }
        });
    }

    protected void changeBarrelState(boolean open) {
        position.dimension().updateBlockProperty(
                BlockPropertyTypes.OPEN_BIT,
                open,
                position.x(),
                position.y(),
                position.z()
        );
        position.dimension().addSound(
                MathUtils.center(position),
                open ? SimpleSound.BARREL_OPEN : SimpleSound.BARREL_CLOSE
        );
    }
}
