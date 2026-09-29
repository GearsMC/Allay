package org.allaymc.server.player;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.Skin;
import org.cloudburstmc.protocol.bedrock.data.skin.*;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SkinConvertor is a utility class to convert between the API's Skin object and the protocol's SerializedSkin object.
 */
@Slf4j
@UtilityClass
public final class SkinConvertor {
    /**
     * Converts an API Skin object into a protocol SerializedSkin object for network transmission.
     *
     * @param skin the API Skin object to convert
     * @return a new SerializedSkin instance containing the data from the Skin object
     */
    public static SerializedSkin toSerializedSkin(Skin skin) {
        // Convert ImageData for skin and cape
        ImageData serializedSkinData = ImageData.of(
                skin.skinData().width(), skin.skinData().height(), skin.skinData().data().clone());

        ImageData serializedCapeData = ImageData.of(
                skin.capeData().width(), skin.capeData().height(), skin.capeData().data().clone());

        // Convert list of animations
        List<AnimationData> serializedAnimations = orEmpty(skin.animations()).stream()
                .map(SkinConvertor::convertAnimationToSerialized)
                .collect(Collectors.toList());

        // Convert list of persona pieces
        List<PersonaPieceData> serializedPersonaPieces = orEmpty(skin.personaPieces()).stream()
                .map(SkinConvertor::convertPersonaPiece)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Convert list of persona piece tint colors
        List<PersonaPieceTintData> serializedTintColors = orEmpty(skin.pieceTintColors()).stream()
                .map(SkinConvertor::convertTintColor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String skinColor = Objects.requireNonNullElse(skin.skinColor(), "#0");

        // Use the SerializedSkin builder to construct the final object
        return SerializedSkin.builder()
                .skinId(skin.skinId())
                .playFabId(Objects.requireNonNullElse(skin.playFabId(), ""))
                .skinResourcePatch(skin.skinResourcePatch())
                .skinData(serializedSkinData)
                .animations(serializedAnimations)
                .capeData(serializedCapeData)
                .geometryData(Objects.requireNonNullElse(skin.skinGeometry(), ""))
                .geometryDataEngineVersion(Objects.requireNonNullElse(skin.geometryDataEngineVersion(), ""))
                .animationData(Objects.requireNonNullElse(skin.animationData(), ""))
                .premium(skin.premiumSkin())
                .persona(skin.personaSkin())
                .capeOnClassic(skin.personaCapeOnClassicSkin())
                .primaryUser(skin.primaryUser())
                .capeId(Objects.requireNonNullElse(skin.capeId(), ""))
                .fullSkinId(fullSkinId(skin))
                .armSize(Objects.requireNonNullElse(skin.armSize(), Skin.ARM_SIZE_WIDE))
                .skinColor(skinColor)
                .color(parseColor(skinColor))
                .personaPieces(serializedPersonaPieces)
                .tintColors(serializedTintColors)
                // GearsMC fork: PocketMine her gonderimde true yollar (SkinData varsayilani). false giden
                // bir gorunumu istemci kendi oyuncusuna uygulamaz, kendi eski gorunumunde kalir; bu yuzden
                // oyuncunun degistirdigi kostum ve takilan pelerin kendi ekraninda hic gorunmuyordu.
                .overridingPlayerAppearance(true)
                .build();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    /**
     * GearsMC fork: gorunumun iceriginden turetilen tam kimlik.
     *
     * <p>Istemci gorunumleri {@code fullSkinId} ile onbellege alir; ayni kimlikle gelen yeni bir
     * gorunumu yok sayip eskisini gostermeye devam eder. Oyuncunun istemciden gelen kimligi ya da
     * {@code toBuilder} ile kopyalanan eski kimlik pelerin takilip cikarildiginda da ayni kaldigi icin
     * {@code /pelerin} ve kostum degisimi ekrana yansimiyordu. PocketMine her {@code SkinData} icin
     * yeni bir UUID uretiyordu; burada icerik ozeti kullanilir: icerik degisince kimlik de degisir,
     * ayni gorunum tekrar gonderildiginde istemci onbellegi bosa gitmez.</p>
     */
    static String fullSkinId(Skin skin) {
        // Bir gorunum degisimi ayni Skin nesnesini her izleyiciye ayri ayri kodlar; ozeti bir kez hesapla.
        var cached = lastFullSkinId;
        if (cached != null && cached.skin() == skin) {
            return cached.id();
        }
        var id = computeFullSkinId(skin);
        lastFullSkinId = new CachedId(skin, id);
        return id;
    }

    private record CachedId(Skin skin, String id) {
    }

    private static volatile CachedId lastFullSkinId;

    private static String computeFullSkinId(Skin skin) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            update(digest, skin.fullId());
            update(digest, skin.skinId());
            update(digest, skin.skinResourcePatch());
            update(digest, skin.skinData());
            update(digest, skin.capeId());
            update(digest, skin.capeData());
            update(digest, skin.skinGeometry());
            update(digest, skin.animationData());
            update(digest, skin.armSize());
            update(digest, skin.skinColor());
            for (var animation : orEmpty(skin.animations())) {
                update(digest, animation.imageData());
                update(digest, animation.animationType().name() + animation.frameCount() + animation.expressionType().name());
            }
            for (var piece : orEmpty(skin.personaPieces())) {
                update(digest, piece.pieceId() + "|" + piece.pieceType() + "|" + piece.packId() + "|" + piece.productId());
            }
            for (var tint : orEmpty(skin.pieceTintColors())) {
                update(digest, tint.pieceType() + "|" + String.join(",", orEmpty(tint.colors())));
            }
            digest.update((byte) ((skin.premiumSkin() ? 1 : 0) | (skin.personaSkin() ? 2 : 0) | (skin.personaCapeOnClassicSkin() ? 4 : 0)));
            return HexFormat.of().formatHex(digest.digest(), 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            return UUID.randomUUID().toString();
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value != null) {
            digest.update(value.getBytes(StandardCharsets.UTF_8));
        }
        digest.update((byte) 0);
    }

    private static void update(MessageDigest digest, Skin.ImageData image) {
        if (image != null) {
            digest.update(ByteBuffer.allocate(8).putInt(image.width()).putInt(image.height()).array());
            digest.update(image.data());
        }
        digest.update((byte) 0);
    }

    /**
     * GearsMC fork: renk tonunu hem eski (metin) hem v2168 (ARGB) alanina acikca yazar.
     *
     * <p>Protokol metinden ARGB'ye cevirirken {@code #rrggbb} bicimini alfa 0 (tamamen saydam) olarak
     * okuyor; oysa kendi ters cevirisi opak renkleri tam da bu bicimde uretiyor. Istemciden gelen bir
     * gorunum sunucudan geri gonderilirken tum persona renkleri boylece saydamlasiyordu.</p>
     */
    private static PersonaPieceTintData convertTintColor(Skin.PersonaPieceTintColor tint) {
        try {
            var colors = new ArrayList<>(orEmpty(tint.colors()));
            var data = new PersonaPieceTintData(tint.pieceType(), colors);
            var argb = new ArrayList<Color>(colors.size());
            for (var color : colors) {
                argb.add(parseColor(color));
            }
            data.setColorsNew(argb);
            return data;
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.debug("Skipping unusable persona tint (type={}): {}", tint.pieceType(), exception.getMessage());
            return null;
        }
    }

    /**
     * {@code #0} saydam, {@code #rrggbb} opak, {@code #aarrggbb} oldugu gibi okunur.
     */
    static Color parseColor(String value) {
        if (value == null) {
            return new Color(0, true);
        }
        var hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            if (hex.isEmpty() || hex.equals("0")) {
                return new Color(0, true);
            }
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                parsed |= 0xFF000000L;
            }
            return new Color((int) parsed, true);
        } catch (NumberFormatException exception) {
            return new Color(0, true);
        }
    }

    /**
     * GearsMC fork: bir persona parcasini protokol nesnesine cevirir, cevrilemezse
     * {@code null} doner.
     *
     * <p>v2168'den once {@code PersonaPieceData} her string'i kabul eden bir record'du.
     * Artik kurucu {@code packId}'yi {@code UUID.fromString} ile ayristiriyor ve parca
     * tipini {@code PersonaPieceType.fromName} ile cozuyor; ikisi de taniyamazsa
     * istisna atiyor. Bu veri dogrudan istemciden geldigi icin bozuk ya da bilinmeyen
     * bir parca butun girisi dusururdu — tek parcayi atlamak deri gorunumunde kucuk
     * bir eksiklige, atlamamak ise oyuncunun sunucuya hic girememesine mal olur.</p>
     *
     * @param piece istemciden gelen parca
     * @return protokol nesnesi, ya da veri gecersizse {@code null}
     */
    private static PersonaPieceData convertPersonaPiece(Skin.PersonaPieces piece) {
        try {
            return new PersonaPieceData(
                    piece.pieceId(),
                    piece.pieceType(),
                    piece.packId(),
                    piece.isDefault(),
                    piece.productId()
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.debug("Skipping unusable persona piece (type={}, packId={}): {}",
                    piece.pieceType(), piece.packId(), exception.getMessage());
            return null;
        }
    }

    /**
     * Converts a protocol SerializedSkin object into an API Skin object.
     *
     * @param serializedSkin the protocol SerializedSkin object to convert
     * @return a new API Skin instance containing the data from the SerializedSkin object
     */
    public static Skin fromSerializedSkin(SerializedSkin serializedSkin) {
        // Convert ImageData for skin and cape
        Skin.ImageData skinData = new Skin.ImageData(
                serializedSkin.getSkinData().getWidth(),
                serializedSkin.getSkinData().getHeight(),
                serializedSkin.getSkinData().getImage().clone()
        );

        Skin.ImageData capeData = new Skin.ImageData(
                serializedSkin.getCapeData().getWidth(),
                serializedSkin.getCapeData().getHeight(),
                serializedSkin.getCapeData().getImage().clone()
        );

        // Convert list of animations
        List<Skin.AnimationData> animations = serializedSkin.getAnimations().stream()
                .map(SkinConvertor::convertAnimationFromSerialized)
                .collect(Collectors.toList());

        // Convert list of persona pieces
        List<Skin.PersonaPieces> personaPieces = serializedSkin.getPersonaPieces().stream()
                .map(piece -> new Skin.PersonaPieces(
                        piece.id(),
                        piece.type(),
                        piece.packId(),
                        piece.isDefault(),
                        piece.productId()
                ))
                .collect(Collectors.toList());

        // Convert list of persona piece tint colors
        List<Skin.PersonaPieceTintColor> tintColors = serializedSkin.getTintColors().stream()
                .map(tint -> new Skin.PersonaPieceTintColor(
                        tint.type(),
                        new ArrayList<>(tint.colors())
                ))
                .collect(Collectors.toList());

        // Construct the API Skin record
        return new Skin(
                serializedSkin.getSkinId(),
                serializedSkin.getPlayFabId(),
                serializedSkin.getSkinResourcePatch(),
                skinData,
                animations,
                capeData,
                serializedSkin.getGeometryData(),
                serializedSkin.getAnimationData(),
                serializedSkin.getGeometryDataEngineVersion(),
                serializedSkin.isPremium(),
                serializedSkin.isPersona(),
                serializedSkin.isCapeOnClassic(),
                serializedSkin.isPrimaryUser(),
                serializedSkin.getCapeId(),
                serializedSkin.getFullSkinId(),
                serializedSkin.getSkinColor(),
                serializedSkin.getArmSize(),
                personaPieces,
                tintColors,
                serializedSkin.isOverridingPlayerAppearance()
        );
    }

    private static AnimationData convertAnimationToSerialized(Skin.AnimationData data) {
        return new AnimationData(
                ImageData.of(
                        data.imageData().width(),
                        data.imageData().height(),
                        data.imageData().data().clone()
                ),
                convertAnimationType(data.animationType()),
                data.frameCount(),
                convertExpressionType(data.expressionType())
        );
    }

    private static Skin.AnimationData convertAnimationFromSerialized(AnimationData data) {
        return new Skin.AnimationData(
                new Skin.ImageData(
                        data.image().getWidth(),
                        data.image().getHeight(),
                        data.image().getImage().clone()
                ),
                convertAnimationType(data.textureType()),
                data.frames(),
                convertExpressionType(data.expressionType())
        );
    }

    private static AnimatedTextureType convertAnimationType(Skin.AnimationType type) {
        return switch (type) {
            case FACE -> AnimatedTextureType.FACE;
            case BODY_32X32 -> AnimatedTextureType.BODY_32X32;
            case BODY_128X128 -> AnimatedTextureType.BODY_128X128;
            default -> AnimatedTextureType.NONE;
        };
    }

    private static Skin.AnimationType convertAnimationType(AnimatedTextureType type) {
        return switch (type) {
            case FACE -> Skin.AnimationType.FACE;
            case BODY_32X32 -> Skin.AnimationType.BODY_32X32;
            case BODY_128X128 -> Skin.AnimationType.BODY_128X128;
            default -> Skin.AnimationType.NONE;
        };
    }

    private static AnimationExpressionType convertExpressionType(Skin.ExpressionType type) {
        return switch (type) {
            case LINEAR -> AnimationExpressionType.LINEAR;
            case BLINKING -> AnimationExpressionType.BLINKING;
        };
    }

    private static Skin.ExpressionType convertExpressionType(AnimationExpressionType type) {
        return switch (type) {
            case LINEAR -> Skin.ExpressionType.LINEAR;
            case BLINKING -> Skin.ExpressionType.BLINKING;
        };
    }
}
