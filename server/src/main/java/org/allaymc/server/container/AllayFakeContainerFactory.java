package org.allaymc.server.container;

import org.allaymc.api.container.FakeContainerFactory;
import org.allaymc.api.container.interfaces.FakeContainer;
import org.allaymc.server.container.impl.FakeChestContainerImpl;
import org.allaymc.server.container.impl.FakeDoubleChestContainerImpl;
import org.allaymc.server.container.impl.FakeHopperContainerImpl;

/**
 * @author daoge_cmd
 */
public class AllayFakeContainerFactory implements FakeContainerFactory {
    @Override
    public FakeContainer createFakeChestContainer() {
        return new FakeChestContainerImpl();
    }

    @Override
    public FakeContainer createFakeDoubleChestContainer() {
        return new FakeDoubleChestContainerImpl();
    }

    @Override
    public FakeContainer createFakeHopperContainer() {
        return new FakeHopperContainerImpl();
    }
}
