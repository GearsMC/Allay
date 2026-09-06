package org.allaymc.api.camera;

import lombok.Getter;

/**
 * The camera presets that vanilla Bedrock ships with.
 *
 * <p>The client only knows a preset by its index in the {@code CameraPresets}
 * packet the server sends on join, so the declaration order here <b>is</b> the
 * wire format: {@link #ordinal()} is the index that goes into a camera
 * instruction. Adding a preset in the middle would silently repoint every
 * instruction, so new presets go at the end.</p>
 *
 * @author GearsMC fork
 */
@Getter
public enum CameraPreset {
    /// A free camera that is not attached to the player at all.
    FREE("minecraft:free", AudioListener.CAMERA),
    FIRST_PERSON("minecraft:first_person", AudioListener.PLAYER),
    THIRD_PERSON("minecraft:third_person", AudioListener.PLAYER),
    THIRD_PERSON_FRONT("minecraft:third_person_front", AudioListener.PLAYER),
    FOLLOW_ORBIT("minecraft:follow_orbit", AudioListener.PLAYER),
    FIXED_BOOM("minecraft:fixed_boom", AudioListener.PLAYER);

    private final String identifier;
    private final AudioListener audioListener;

    CameraPreset(String identifier, AudioListener audioListener) {
        this.identifier = identifier;
        this.audioListener = audioListener;
    }

    /**
     * Where sound is heard from while this preset is active.
     */
    public enum AudioListener {
        /// Sound is heard from the camera.
        CAMERA,
        /// Sound is heard from the player.
        PLAYER
    }
}
