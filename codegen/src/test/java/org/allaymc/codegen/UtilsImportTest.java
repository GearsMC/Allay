package org.allaymc.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link Utils#collapseImports} davranışı: üretilen dosyalarda import'lar IDE'deki gibi toplanmalı. */
class UtilsImportTest {

    @Test
    void collapsesPackagesWithAtLeastFiveImports() {
        var source = """
                package org.allaymc.api.item.type;

                import org.allaymc.api.item.interfaces.ItemAirStack;
                import org.allaymc.api.item.interfaces.ItemBoatStack;
                import org.allaymc.api.item.interfaces.ItemCakeStack;
                import org.allaymc.api.item.interfaces.ItemDoorStack;
                import org.allaymc.api.item.interfaces.ItemEggStack;
                import org.allaymc.api.annotation.MinecraftVersionSensitive;

                public final class ItemTypes {
                }
                """;
        assertEquals("""
                package org.allaymc.api.item.type;

                import org.allaymc.api.annotation.MinecraftVersionSensitive;
                import org.allaymc.api.item.interfaces.*;

                public final class ItemTypes {
                }
                """, Utils.collapseImports(source));
    }

    @Test
    void keepsFewImportsAndStaticImportsUntouched() {
        var source = """
                package a;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import java.nio.file.Files;
                import java.nio.file.Path;

                class A {
                }
                """;
        assertEquals(source, Utils.collapseImports(source));
    }
}
