package org.allaymc.api.container;

import org.allaymc.api.AllayAPI;
import org.allaymc.api.container.interfaces.FakeContainer;

/**
 * FakeContainerFactory is a factory for creating {@link FakeContainer} instances.
 *
 * @author daoge_cmd
 */
public interface FakeContainerFactory {
    AllayAPI.APIInstanceHolder<FakeContainerFactory> FACTORY = AllayAPI.APIInstanceHolder.create();

    static FakeContainerFactory getFactory() {
        return FACTORY.get();
    }

    /**
     * Creates an instance of a chest {@code FakeContainer}.
     *
     * @return A new chest {@code FakeContainer} instance.
     */
    FakeContainer createFakeChestContainer();

    /**
     * Creates an instance of a double chest {@code FakeContainer}.
     * A double chest container typically has twice the storage capacity of a regular chest container.
     *
     * @return A new double chest {@code FakeContainer} instance.
     */
    FakeContainer createFakeDoubleChestContainer();

    /**
     * Creates an instance of a hopper {@code FakeContainer}.
     * A hopper container exposes exactly five slots, which suits compact menus
     * that would otherwise waste most of a chest grid.
     *
     * @return A new hopper {@code FakeContainer} instance.
     */
    FakeContainer createFakeHopperContainer();
}
