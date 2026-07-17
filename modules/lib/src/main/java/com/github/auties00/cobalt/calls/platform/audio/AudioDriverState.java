package com.github.auties00.cobalt.calls.platform.audio;

import java.util.Optional;

/**
 * Enumerates the three lifecycle states shared by the call audio capture and playback drivers.
 *
 * <p>An audio driver is a strict three state machine. A freshly constructed driver is
 * {@link #UNINITIALIZED}; initialization binds a device and records the requested format, moving it to
 * {@link #INITIALIZED}; a start begins streaming samples, moving it to {@link #ACTIVE}; a stop returns it
 * to {@link #INITIALIZED}. The transitions are linear and adjacent: no path skips a state, and a start
 * before initialization is a recoverable fault rather than a state change. Each constant binds the
 * {@link #code() integer code} the driver stores in its state field.
 *
 * <p>The {@link #code()} coincides with this enum's {@link Enum#ordinal()} because the codes are
 * contiguous from {@code 0} to {@code 2} and the constants are declared here in numeric order; the
 * explicit accessor is nonetheless the contract, since declaration order is not a protocol guarantee.
 * This type only names the states shared by {@link AudioCaptureDriver} and {@link AudioPlaybackDriver};
 * the per driver methods enforce which transitions are legal.
 *
 * <p>The state codes on the wire are:
 * {@snippet lang="text" :
 * 0 = UNINITIALIZED   no device bound, no format recorded
 * 1 = INITIALIZED     device bound and format recorded, not streaming
 * 2 = ACTIVE          streaming samples to or from the bound device
 * }
 */
public enum AudioDriverState {
    /**
     * Represents a constructed driver that holds no device and no format: the initial state.
     *
     * <p>Only initialization is legal here; a start in this state is rejected as a recoverable fault.
     */
    UNINITIALIZED(0),

    /**
     * Represents a driver that has bound a device and recorded its capture or playback format but is not
     * streaming samples.
     *
     * <p>A start moves the driver to {@link #ACTIVE}; a stop from {@link #ACTIVE} returns it here; a
     * second initialization re binds the device and stays here.
     */
    INITIALIZED(1),

    /**
     * Represents a driver that is streaming samples to or from its bound device.
     *
     * <p>This is the running state entered by a start. A stop returns the driver to {@link #INITIALIZED}
     * without releasing the device.
     */
    ACTIVE(2);

    /**
     * Holds the integer code the driver stores in its state field.
     */
    private final int code;

    /**
     * Constructs a state constant bound to its integer code.
     *
     * @param code the integer code stored in the driver state field
     */
    AudioDriverState(int code) {
        this.code = code;
    }

    /**
     * Returns the integer code the driver stores in its state field.
     *
     * @return the state code, in the range {@code 0} to {@code 2}
     */
    public int code() {
        return code;
    }

    /**
     * Looks up the state for a state code.
     *
     * <p>The lookup is keyed on the protocol value. A value outside the range {@code 0} to {@code 2}
     * yields an empty result.
     *
     * @param code the state code
     * @return the matching state, or an empty result when the value is out of range
     */
    public static Optional<AudioDriverState> ofCode(int code) {
        if (code < 0 || code >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[code]);
    }
}
