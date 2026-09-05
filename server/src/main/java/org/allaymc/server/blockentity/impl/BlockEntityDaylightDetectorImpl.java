package org.allaymc.server.blockentity.impl;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.interfaces.BlockEntityDaylightDetector;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockEntityDaylightDetectorImpl extends BlockEntityImpl implements BlockEntityDaylightDetector {
    public BlockEntityDaylightDetectorImpl(BlockEntityInitInfo initInfo,
                                 List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
