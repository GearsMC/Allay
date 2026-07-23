package org.allaymc.server.item;

import org.allaymc.api.item.CustomItemFactory;
import org.allaymc.api.item.data.ItemData;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.item.impl.ItemStackImpl;
import org.allaymc.server.item.type.AllayItemType;
import org.allaymc.server.item.type.CustomItemDefinitionGenerator;

/**
 * Default implementation of {@link CustomItemFactory}.
 *
 * @author GearsMC
 */
public class AllayCustomItemFactory implements CustomItemFactory {

    @Override
    public ItemType<?> registerSimpleItem(String identifier, String texture, String displayName,
                                          int maxStackSize, boolean foil) {
        return AllayItemType.builder(ItemStackImpl.class)
                .identifier(new Identifier(identifier))
                .itemData(ItemData.builder().maxStackSize(Math.max(1, Math.min(64, maxStackSize))).build())
                .itemDefinitionGenerator(CustomItemDefinitionGenerator.builder()
                        .texture(texture)
                        .displayName(displayName)
                        .foil(foil)
                        .build())
                .build();
    }
}
