package org.allaymc.server.registry.loader;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmithingTemplateDuplicationFilterTest {

    @Test
    void sablonCogaltmaTarifiAtlanir() {
        var json = JsonParser.parseString("""
                {
                  "id": "minecraft:netherite_upgrade_smithing_template_duplicate",
                  "output": [{"count": 2, "item": "minecraft:netherite_upgrade_smithing_template"}],
                  "tag": "crafting_table"
                }
                """).getAsJsonObject();
        assertTrue(RecipeRegistryLoader.isSmithingTemplateDuplication(json));
    }

    @Test
    void ciktisiSablonOlanTarifAtlanir() {
        var json = JsonParser.parseString("""
                {
                  "id": "minecraft:some_recipe",
                  "output": [{"count": 2, "item": "minecraft:wild_armor_trim_smithing_template"}],
                  "tag": "crafting_table"
                }
                """).getAsJsonObject();
        assertTrue(RecipeRegistryLoader.isSmithingTemplateDuplication(json));
    }

    @Test
    void normalTarifKalir() {
        var json = JsonParser.parseString("""
                {
                  "id": "minecraft:crafting_table",
                  "output": [{"count": 1, "item": "minecraft:crafting_table"}],
                  "tag": "crafting_table"
                }
                """).getAsJsonObject();
        assertFalse(RecipeRegistryLoader.isSmithingTemplateDuplication(json));
    }
}
