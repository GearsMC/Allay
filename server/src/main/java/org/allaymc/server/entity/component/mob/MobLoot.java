package org.allaymc.server.entity.component.mob;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemType;

import java.util.List;
import java.util.function.ToIntFunction;

@FunctionalInterface
public interface MobLoot {

    MobLoot NONE = (context, drops) -> {
    };

    ToIntFunction<LootContext> HOSTILE_XP = context -> context.killedByPlayer() ? 5 : 0;

    ToIntFunction<LootContext> ANIMAL_XP = context -> context.killedByPlayer() && !context.baby()
            ? context.random().nextInt(1, 4) : 0;

    ToIntFunction<LootContext> NO_XP = context -> 0;

    static ToIntFunction<LootContext> xp(int amount) {
        return context -> context.killedByPlayer() ? amount : 0;
    }

    static void add(List<ItemStack> drops, ItemType<?> type, int count) {
        if (type != null && count > 0) {
            drops.add(type.createItemStack(count));
        }
    }

    static void addCooked(List<ItemStack> drops, LootContext context, ItemType<?> raw, ItemType<?> cooked, int count) {
        add(drops, context.onFire() ? cooked : raw, count);
    }

    void roll(LootContext context, List<ItemStack> drops);
}
