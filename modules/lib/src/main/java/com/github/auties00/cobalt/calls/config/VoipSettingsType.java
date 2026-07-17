package com.github.auties00.cobalt.calls.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the target settings selector under which the voip engine stores and activates a raw
 * voip param set.
 *
 * <p>The engine can hold several parsed voip param sets at once, one per media context, and selects
 * exactly one of them as the in use set for the live call. This enum is the three value key that
 * distinguishes those sets: {@link #NONE} is the call wide default bundle that every call starts
 * from, {@link #AUDIO} is the audio only call overlay, and {@link #VIDEO} is the video call overlay.
 * The engine swaps the active set by this key when the call's media mode changes, so the same
 * manager can carry a default set plus the audio and video specialisations and promote whichever one
 * matches the current call.
 *
 * <p>The numeric {@linkplain #code() codes} are {@code 0} for {@link #NONE}, {@code 1} for
 * {@link #AUDIO}, and {@code 2} for {@link #VIDEO}; they are contiguous and match the order the
 * engine uses when indexing its target settings name table.
 */
public enum VoipSettingsType {
    /**
     * The call wide default bundle keyed under {@linkplain #code() code} {@code 0}.
     *
     * <p>This is the mandatory baseline set every call is filled from before any audio specific or
     * video specific overlay is selected.
     */
    NONE(0, "none"),

    /**
     * The audio call overlay keyed under {@linkplain #code() code} {@code 1}.
     */
    AUDIO(1, "audio"),

    /**
     * The video call overlay keyed under {@linkplain #code() code} {@code 2}.
     */
    VIDEO(2, "video");

    /**
     * Resolves an engine target settings code to its set, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * set in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, VoipSettingsType> BY_CODE;

    static {
        var byCode = new HashMap<Integer, VoipSettingsType>();
        for (var type : values()) {
            if (byCode.put(type.code, type) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * The integer target settings code the engine indexes this set by.
     */
    private final int code;

    /**
     * The lowercase token the engine prints for this set.
     */
    private final String token;

    /**
     * Constructs a target settings constant bound to its engine code and printable token.
     *
     * @param code  the integer target settings code the engine indexes by
     * @param token the lowercase token the engine prints for this set
     */
    VoipSettingsType(int code, String token) {
        this.code = code;
        this.token = token;
    }

    /**
     * Returns the integer target settings code the engine indexes this set by.
     *
     * @return the engine target settings code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the lowercase token the engine prints for this set.
     *
     * <p>The token is one of {@code "none"}, {@code "audio"}, or {@code "video"}, matching the
     * constant it belongs to.
     *
     * @return the printable token for this set
     */
    public String token() {
        return token;
    }

    /**
     * Returns the target settings set whose {@linkplain #code() code} equals the given value.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}.
     * @param code the engine target settings code to resolve
     * @return the matching set, or {@link Optional#empty()} if no set matches
     */
    public static Optional<VoipSettingsType> ofCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
