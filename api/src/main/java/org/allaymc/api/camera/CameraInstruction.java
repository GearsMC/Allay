package org.allaymc.api.camera;

import lombok.Getter;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A single camera instruction: which preset to switch to, where to put the
 * camera and how to get there.
 *
 * <p>Built fluently, in the same style as {@link org.allaymc.api.dialog.Dialog}:</p>
 * <pre>{@code
 * player.viewCamera(CameraInstruction.preset(CameraPreset.FREE)
 *         .ease(CameraEaseType.LINEAR, 4.0f)
 *         .position(250, 120, 257)
 *         .facing(250, 98, 257));
 * }</pre>
 *
 * <p>{@link #facing} and {@link #rotation} are alternatives: facing keeps the
 * camera pointed at a fixed point for the whole movement, rotation fixes the
 * angles instead. Setting both leaves it to the client, which honours facing.</p>
 *
 * @author GearsMC fork
 */
@Getter
public final class CameraInstruction {

    private final CameraPreset preset;

    private CameraEaseType easeType;
    private float easeSeconds;
    private Vector3dc position;
    private Vector3dc facing;
    private Double pitch;
    private Double yaw;

    private CameraInstruction(CameraPreset preset) {
        this.preset = preset;
    }

    /**
     * Starts an instruction that switches to the given preset.
     *
     * @param preset the preset to switch to
     * @return a new instruction
     */
    public static CameraInstruction preset(CameraPreset preset) {
        if (preset == null) {
            throw new IllegalArgumentException("preset cannot be null");
        }
        return new CameraInstruction(preset);
    }

    /**
     * Eases the camera into its new position instead of cutting to it.
     *
     * @param type    the easing curve
     * @param seconds how long the movement takes
     * @return this instruction
     */
    public CameraInstruction ease(CameraEaseType type, float seconds) {
        this.easeType = type;
        this.easeSeconds = seconds;
        return this;
    }

    /**
     * Places the camera at the given world position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return this instruction
     */
    public CameraInstruction position(double x, double y, double z) {
        this.position = new Vector3d(x, y, z);
        return this;
    }

    /**
     * Keeps the camera pointed at the given world position for the whole movement.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return this instruction
     */
    public CameraInstruction facing(double x, double y, double z) {
        this.facing = new Vector3d(x, y, z);
        return this;
    }

    /**
     * Fixes the camera angles instead of pointing it at a position.
     *
     * @param pitch the pitch in degrees
     * @param yaw   the yaw in degrees
     * @return this instruction
     */
    public CameraInstruction rotation(double pitch, double yaw) {
        this.pitch = pitch;
        this.yaw = yaw;
        return this;
    }

    /**
     * @return {@code true} if this instruction eases instead of cutting
     */
    public boolean hasEase() {
        return easeType != null;
    }

    /**
     * @return {@code true} if fixed angles were set
     */
    public boolean hasRotation() {
        return pitch != null && yaw != null;
    }
}
