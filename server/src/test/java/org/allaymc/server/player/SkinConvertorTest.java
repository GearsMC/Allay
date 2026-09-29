package org.allaymc.server.player;

import org.allaymc.api.player.Skin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinConvertorTest {

    @Test
    void conversionsDeepCopyMutableSkinPayloads() {
        byte[] skinPixels = {1, 2};
        byte[] capePixels = {3, 4};
        byte[] animationPixels = {5, 6};
        var tintColors = new ArrayList<>(List.of("#111111", "#222222"));
        var skin = createSkin(skinPixels, capePixels, animationPixels, tintColors);

        var serialized = SkinConvertor.toSerializedSkin(skin);
        assertNotSame(skinPixels, serialized.getSkinData().getImage());
        assertNotSame(capePixels, serialized.getCapeData().getImage());
        assertNotSame(animationPixels, serialized.getAnimations().getFirst().image().getImage());
        assertNotSame(tintColors, serialized.getTintColors().getFirst().colors());

        skinPixels[0] = 21;
        capePixels[0] = 22;
        animationPixels[0] = 23;
        tintColors.set(0, "#source");
        assertEquals(1, serialized.getSkinData().getImage()[0]);
        assertEquals(3, serialized.getCapeData().getImage()[0]);
        assertEquals(5, serialized.getAnimations().getFirst().image().getImage()[0]);
        assertEquals("#111111", serialized.getTintColors().getFirst().colors().getFirst());

        serialized.getSkinData().getImage()[1] = 31;
        serialized.getCapeData().getImage()[1] = 32;
        serialized.getAnimations().getFirst().image().getImage()[1] = 33;
        serialized.getTintColors().getFirst().colors().set(1, "#packet");
        assertEquals(2, skinPixels[1]);
        assertEquals(4, capePixels[1]);
        assertEquals(6, animationPixels[1]);
        assertEquals("#222222", tintColors.get(1));

        var convertedBack = SkinConvertor.fromSerializedSkin(serialized);
        assertNotSame(serialized.getSkinData().getImage(), convertedBack.skinData().data());
        assertNotSame(serialized.getCapeData().getImage(), convertedBack.capeData().data());
        assertNotSame(
                serialized.getAnimations().getFirst().image().getImage(),
                convertedBack.animations().getFirst().imageData().data()
        );
        assertNotSame(serialized.getTintColors().getFirst().colors(), convertedBack.pieceTintColors().getFirst().colors());

        byte copiedSkinPixel = convertedBack.skinData().data()[0];
        byte copiedAnimationPixel = convertedBack.animations().getFirst().imageData().data()[0];
        String copiedTint = convertedBack.pieceTintColors().getFirst().colors().getFirst();
        serialized.getSkinData().getImage()[0] = 41;
        serialized.getAnimations().getFirst().image().getImage()[0] = 42;
        serialized.getTintColors().getFirst().colors().set(0, "#protocol");
        assertEquals(copiedSkinPixel, convertedBack.skinData().data()[0]);
        assertEquals(copiedAnimationPixel, convertedBack.animations().getFirst().imageData().data()[0]);
        assertEquals(copiedTint, convertedBack.pieceTintColors().getFirst().colors().getFirst());

        byte serializedCapePixel = serialized.getCapeData().getImage()[0];
        String serializedTint = serialized.getTintColors().getFirst().colors().get(1);
        convertedBack.capeData().data()[0] = 51;
        convertedBack.pieceTintColors().getFirst().colors().set(1, "#api");
        assertEquals(serializedCapePixel, serialized.getCapeData().getImage()[0]);
        assertEquals(serializedTint, serialized.getTintColors().getFirst().colors().get(1));

        var second = SkinConvertor.toSerializedSkin(skin);
        assertNotSame(serialized.getSkinData().getImage(), second.getSkinData().getImage());
        assertNotSame(serialized.getTintColors().getFirst().colors(), second.getTintColors().getFirst().colors());
    }

    @Test
    void opaqueColorsSurviveRoundTripThroughClientSkin() {
        var skin = createSkin(new byte[]{1}, new byte[]{2}, new byte[]{3}, new ArrayList<>(List.of("#ff112233", "#0", "#445566", "#80aabbcc")));

        var serialized = SkinConvertor.toSerializedSkin(skin);
        assertEquals(0xFFABCDEF, serialized.getColor().getRGB());
        var colors = serialized.getTintColors().getFirst().getColorsNew();
        assertEquals(0xFF112233, colors.get(0).getRGB());
        assertEquals(0, colors.get(1).getAlpha());
        assertEquals(0xFF445566, colors.get(2).getRGB());
        assertEquals(0x80AABBCC, colors.get(3).getRGB());

        // Istemciden gelen v2168 gorunumu metne cevrilip geri gonderildiginde renkler saydamlasmamali.
        var echoed = SkinConvertor.toSerializedSkin(SkinConvertor.fromSerializedSkin(serialized));
        assertEquals(0xFFABCDEF, echoed.getColor().getRGB());
        assertEquals(0xFF112233, echoed.getTintColors().getFirst().getColorsNew().getFirst().getRGB());
    }

    @Test
    void fullSkinIdFollowsSkinContent() {
        var skin = createSkin(new byte[]{1}, new byte[]{2}, new byte[]{3}, new ArrayList<>(List.of("#111111")));
        var sameContent = createSkin(new byte[]{1}, new byte[]{2}, new byte[]{3}, new ArrayList<>(List.of("#111111")));
        var withOtherCape = skin.toBuilder()
                .capeId("gears:goldage")
                .capeData(new Skin.ImageData(1, 1, new byte[]{9}))
                .build();
        var withOtherPixels = skin.toBuilder().skinData(new Skin.ImageData(1, 1, new byte[]{7})).build();

        String id = SkinConvertor.toSerializedSkin(skin).getFullSkinId();
        assertEquals(id, SkinConvertor.toSerializedSkin(sameContent).getFullSkinId());
        assertNotEquals(id, SkinConvertor.toSerializedSkin(withOtherCape).getFullSkinId());
        assertNotEquals(id, SkinConvertor.toSerializedSkin(withOtherPixels).getFullSkinId());
    }

    @Test
    void encodedSkinAlwaysOverridesClientAppearance() {
        var skin = createSkin(new byte[]{1}, new byte[]{2}, new byte[]{3}, new ArrayList<>())
                .toBuilder()
                .overrideAppearance(false)
                .build();

        assertTrue(SkinConvertor.toSerializedSkin(skin).isOverridingPlayerAppearance());
    }

    private static Skin createSkin(
            byte[] skinPixels,
            byte[] capePixels,
            byte[] animationPixels,
            List<String> tintColors
    ) {
        return Skin.builder()
                .skinId("skin-id")
                .playFabId("playfab-id")
                .skinResourcePatch("{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}")
                .skinData(new Skin.ImageData(1, 1, skinPixels))
                .animations(new ArrayList<>(List.of(new Skin.AnimationData(
                        new Skin.ImageData(1, 1, animationPixels),
                        Skin.AnimationType.FACE,
                        1,
                        Skin.ExpressionType.LINEAR
                ))))
                .capeData(new Skin.ImageData(1, 1, capePixels))
                .skinGeometry("geometry-data")
                .animationData("animation-data")
                .geometryDataEngineVersion("1.0.0")
                .premiumSkin(false)
                .personaSkin(true)
                .personaCapeOnClassicSkin(false)
                .primaryUser(true)
                .capeId("cape-id")
                .fullId("full-id")
                .skinColor("#abcdef")
                .armSize(Skin.ARM_SIZE_WIDE)
                .personaPieces(new ArrayList<>(List.of(new Skin.PersonaPieces(
                        "piece-id", "persona_hair", "pack-id", false, "product-id"
                ))))
                .pieceTintColors(new ArrayList<>(List.of(new Skin.PersonaPieceTintColor(
                        "persona_hair", tintColors
                ))))
                .overrideAppearance(true)
                .build();
    }
}
