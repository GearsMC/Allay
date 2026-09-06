package org.allaymc.server.item.recipe;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.recipe.Recipe;
import org.allaymc.api.item.recipe.ShapedRecipe;
import org.allaymc.api.item.recipe.ShapelessRecipe;
import org.allaymc.api.item.recipe.input.CraftingRecipeInput;
import org.allaymc.api.registry.Registries;

public final class CraftingRecipeMatcher {

    private CraftingRecipeMatcher() {
    }

    public static Recipe match(CraftingRecipeInput input, Recipe hint) {
        if (hint != null && isCraftingRecipe(hint) && hint.match(input) && outputsOf(hint, input) != null) {
            return hint;
        }

        Recipe best = null;
        for (var recipe : Registries.RECIPES.getContent().values()) {
            if (!isCraftingRecipe(recipe) || !recipe.match(input)) {
                continue;
            }

            if (best == null || recipe.getPriority() < best.getPriority()) {
                best = recipe;
            }
        }

        return best != null && outputsOf(best, input) != null ? best : null;
    }

    public static ItemStack[] outputsOf(Recipe recipe, CraftingRecipeInput input) {
        return recipe instanceof ComplexRecipe complex ? complex.getOutputs(input) : recipe.getOutputs();
    }

    private static boolean isCraftingRecipe(Recipe recipe) {
        if (recipe instanceof ShapelessRecipe shapeless) {
            return shapeless.getType() == ShapelessRecipe.Type.CRAFTING;
        }

        return recipe instanceof ShapedRecipe || recipe instanceof ComplexRecipe;
    }
}
