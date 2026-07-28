package org.allaymc.server.item.creative;

import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.item.creative.CreativeItemCategory;
import org.allaymc.api.item.creative.CreativeItemCategory.Type;
import org.allaymc.api.item.creative.CreativeItemEntry;
import org.allaymc.api.item.creative.CreativeItemGroup;
import org.allaymc.api.item.creative.CreativeItemRegistry;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.Utils;
import org.allaymc.api.utils.identifier.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author daoge_cmd
 */
@Slf4j
public class AllayCreativeItemRegistry implements CreativeItemRegistry {

    protected final Map<Type, AllayCreativeItemCategory> categories;
    protected final List<CreativeItemEntry> entries;
    protected final List<CreativeItemGroup> groups;

    public AllayCreativeItemRegistry() {
        this.categories = new EnumMap<>(Type.class);
        this.categories.put(Type.CONSTRUCTION, new AllayCreativeItemCategory(this, Type.CONSTRUCTION));
        this.categories.put(Type.NATURE, new AllayCreativeItemCategory(this, Type.NATURE));
        this.categories.put(Type.EQUIPMENT, new AllayCreativeItemCategory(this, Type.EQUIPMENT));
        this.categories.put(Type.ITEMS, new AllayCreativeItemCategory(this, Type.ITEMS));
        this.entries = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.load();
    }

    @SneakyThrows
    protected void load() {
        // Load groups
        try (var reader = new InputStreamReader(new BufferedInputStream(Utils.getResource("creative_groups.json")))) {
            JsonParser.parseReader(reader).getAsJsonArray().forEach(entry -> {
                var group = entry.getAsJsonObject();
                var category = getCategory(Type.valueOf(group.get("category").getAsString().toUpperCase(Locale.ROOT)));
                var name = group.get("name").getAsString();
                if (name.isEmpty()) {
                    category.registerUnnamedGroup();
                } else {
                    var iconItemTypeName = new Identifier(group.get("icon").getAsString());
                    var iconItemType = Registries.ITEMS.get(iconItemTypeName);
                    Objects.requireNonNull(iconItemType, "Unknown icon item type: " + iconItemTypeName);
                    category.registerGroup(name, iconItemType.createItemStack());
                }
            });
        }

        // Load items
        try (var reader = NbtUtils.createGZIPReader(new BufferedInputStream(Utils.getResource("creative_items.nbt")))) {
            var items = ((NbtMap) reader.readTag()).getList("items", NbtType.COMPOUND);
            for (var item : items) {
                var itemTypeName = new Identifier(item.getString("name"));
                var itemType = Registries.ITEMS.get(itemTypeName);
                Objects.requireNonNull(itemType, "Unknown item type: " + itemTypeName);

                var category = getCategory(Type.valueOf(item.getString("category").toUpperCase(Locale.ROOT)));
                var itemStack = itemType.createItemStack(
                        ItemStackInitInfo
                                .builder().count(1).meta(item.getShort("damage"))
                                .extraTag(item.getCompound("tag", NbtMap.builder().build()))
                                .assignUniqueId(false).build()
                );
                int groupIndex = (int) item.getLong("groupIndex");
                var group = category.getGroup(groupIndex);
                if (group == null) {
                    log.warn("Unknown group index {} for item {} in category {}!", groupIndex, itemTypeName, category.getType());
                    continue;
                }

                group.registerItem(itemStack);
            }
        }

        // Education/chemistry content is missing from the Endstone creative dump used above.
        registerChemistryItems();
    }

    /**
     * Registers Education Edition chemistry blocks and items into the creative inventory.
     * Vanilla {@code creative_items.nbt} comes from a non-Education export, so these never appear
     * unless added here. Same placeable stubs PocketMine enables via chemistry resource packs.
     */
    private void registerChemistryItems() {
        var construction = getCategory(Type.CONSTRUCTION);
        var items = getCategory(Type.ITEMS);

        var tables = construction.registerGroup("itemGroup.name.chemistryTables", stack(ItemTypes.COMPOUND_CREATOR, 0));
        register(tables,
                ItemTypes.COMPOUND_CREATOR,
                ItemTypes.ELEMENT_CONSTRUCTOR,
                ItemTypes.LAB_TABLE,
                ItemTypes.MATERIAL_REDUCER,
                ItemTypes.CHEMICAL_HEAT
        );

        var elements = construction.registerGroup("itemGroup.name.elements", stack(ItemTypes.ELEMENT_1, 0));
        for (int atomicNumber = 0; atomicNumber <= 118; atomicNumber++) {
            var elementType = Registries.ITEMS.get(new Identifier("minecraft:element_" + atomicNumber));
            if (elementType != null) {
                elements.registerItem(stack(elementType, 0));
            }
        }

        var hardGlass = construction.registerGroup("itemGroup.name.hardGlass", stack(ItemTypes.HARD_GLASS, 0));
        register(hardGlass,
                ItemTypes.HARD_GLASS,
                ItemTypes.HARD_GLASS_PANE,
                ItemTypes.HARD_WHITE_STAINED_GLASS,
                ItemTypes.HARD_ORANGE_STAINED_GLASS,
                ItemTypes.HARD_MAGENTA_STAINED_GLASS,
                ItemTypes.HARD_LIGHT_BLUE_STAINED_GLASS,
                ItemTypes.HARD_YELLOW_STAINED_GLASS,
                ItemTypes.HARD_LIME_STAINED_GLASS,
                ItemTypes.HARD_PINK_STAINED_GLASS,
                ItemTypes.HARD_GRAY_STAINED_GLASS,
                ItemTypes.HARD_LIGHT_GRAY_STAINED_GLASS,
                ItemTypes.HARD_CYAN_STAINED_GLASS,
                ItemTypes.HARD_PURPLE_STAINED_GLASS,
                ItemTypes.HARD_BLUE_STAINED_GLASS,
                ItemTypes.HARD_BROWN_STAINED_GLASS,
                ItemTypes.HARD_GREEN_STAINED_GLASS,
                ItemTypes.HARD_RED_STAINED_GLASS,
                ItemTypes.HARD_BLACK_STAINED_GLASS,
                ItemTypes.HARD_WHITE_STAINED_GLASS_PANE,
                ItemTypes.HARD_ORANGE_STAINED_GLASS_PANE,
                ItemTypes.HARD_MAGENTA_STAINED_GLASS_PANE,
                ItemTypes.HARD_LIGHT_BLUE_STAINED_GLASS_PANE,
                ItemTypes.HARD_YELLOW_STAINED_GLASS_PANE,
                ItemTypes.HARD_LIME_STAINED_GLASS_PANE,
                ItemTypes.HARD_PINK_STAINED_GLASS_PANE,
                ItemTypes.HARD_GRAY_STAINED_GLASS_PANE,
                ItemTypes.HARD_LIGHT_GRAY_STAINED_GLASS_PANE,
                ItemTypes.HARD_CYAN_STAINED_GLASS_PANE,
                ItemTypes.HARD_PURPLE_STAINED_GLASS_PANE,
                ItemTypes.HARD_BLUE_STAINED_GLASS_PANE,
                ItemTypes.HARD_BROWN_STAINED_GLASS_PANE,
                ItemTypes.HARD_GREEN_STAINED_GLASS_PANE,
                ItemTypes.HARD_RED_STAINED_GLASS_PANE,
                ItemTypes.HARD_BLACK_STAINED_GLASS_PANE
        );

        var torches = items.registerGroup("itemGroup.name.chemistryTorches", stack(ItemTypes.UNDERWATER_TORCH, 0));
        register(torches,
                ItemTypes.UNDERWATER_TORCH,
                ItemTypes.COLORED_TORCH_BLUE,
                ItemTypes.COLORED_TORCH_RED,
                ItemTypes.COLORED_TORCH_GREEN,
                ItemTypes.COLORED_TORCH_PURPLE,
                ItemTypes.UNDERWATER_TNT
        );

        var chemistryItems = items.registerGroup("itemGroup.name.chemistryItems", stack(ItemTypes.BLEACH, 0));
        register(chemistryItems,
                ItemTypes.BLEACH,
                ItemTypes.ICE_BOMB,
                ItemTypes.RAPID_FERTILIZER,
                ItemTypes.CAMERA,
                ItemTypes.ALLOW,
                ItemTypes.DENY,
                ItemTypes.BORDER_BLOCK
        );
        // Compound / medicine / balloon / sparkler / glow stick use damage/meta variants.
        for (int meta = 0; meta <= 37; meta++) {
            chemistryItems.registerItem(stack(ItemTypes.COMPOUND, meta));
        }
        for (int meta = 0; meta <= 3; meta++) {
            chemistryItems.registerItem(stack(ItemTypes.MEDICINE, meta));
        }
        for (int meta = 0; meta <= 15; meta++) {
            chemistryItems.registerItem(stack(ItemTypes.BALLOON, meta));
            chemistryItems.registerItem(stack(ItemTypes.SPARKLER, meta));
            chemistryItems.registerItem(stack(ItemTypes.GLOW_STICK, meta));
        }
    }

    private static void register(CreativeItemGroup group, ItemType<?>... types) {
        for (var type : types) {
            if (type != null) {
                group.registerItem(stack(type, 0));
            }
        }
    }

    private static ItemStack stack(ItemType<?> type, int meta) {
        return type.createItemStack(
                ItemStackInitInfo.builder().count(1).meta(meta).assignUniqueId(false).build()
        );
    }

    @Override
    public CreativeItemCategory getCategory(Type type) {
        return categories.get(type);
    }

    @Override
    public CreativeItemEntry getEntryByIndex(int index) {
        return entries.get(index);
    }

    @Override
    public CreativeItemGroup getGroupByIndex(int index) {
        return groups.get(index);
    }

    @Override
    public List<CreativeItemEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public List<CreativeItemGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    CreativeItemEntry assignIndexForEntry(CreativeItemGroup group, ItemStack itemStack) {
        var entry = new CreativeItemEntry(entries.size(), group, itemStack);
        entries.add(entry);
        return entry;
    }

    int assignIndexForGroup(CreativeItemGroup group) {
        groups.add(group);
        return groups.size() - 1;
    }
}
