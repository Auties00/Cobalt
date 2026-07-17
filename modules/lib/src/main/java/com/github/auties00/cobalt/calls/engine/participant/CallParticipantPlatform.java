package com.github.auties00.cobalt.calls.engine.participant;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the client platform the engine attributes to a call participant.
 *
 * <p>Every participant carries a platform code that identifies the kind of WhatsApp
 * client the peer is running, surfaced as the {@code platform} attribute on its
 * membership stanza and exposed through the participant view. The seventeen recognized
 * platforms span the mobile apps ({@link #ANDROID}, {@link #IPHONE}, {@link #IOS_TABLET},
 * {@link #IPAD}, {@link #KAIOS}, {@link #WP}, {@link #WEARM}), the desktop clients
 * ({@link #WINDOWS}, {@link #MACOS}, {@link #MAC_OS_ELECTRON}, {@link #WINDOWS_ELECTRON},
 * {@link #PORTAL}), the web client ({@link #WEB}), the business apps ({@link #SMBA},
 * {@link #SMBI}), the Cloud API client ({@link #CAPI}), and the {@link #UNKNOWN}
 * sentinel.
 *
 * <p>Each constant carries the {@link #code() integer code} the engine stores and the
 * lowercase {@link #token() wire token} the engine emits. The codes are dense over the
 * range {@code 0..16} and map to the tokens in this order:
 *
 * {@snippet lang = "text":
 *  0  unknown            9  windows_electron
 *  1  android           10  wearm
 *  2  iphone            11  macos
 *  3  wp                12  capi
 *  4  ios_tablet        13  ipad
 *  5  kaios             14  smba
 *  6  windows           15  smbi
 *  7  portal            16  web
 *  8  mac_os_electron
 *}
 *
 * <p>The {@link #UNKNOWN} constant doubles as the fallback for any code that falls outside
 * this range: {@link #ofCode(int)} resolves any code other than {@code 0..16} to
 * {@link #UNKNOWN}, matching the engine's own {@code "unknown"} default.
 */
public enum CallParticipantPlatform {
    /**
     * An unrecognized or unspecified platform.
     *
     * <p>This is also the fallback the engine reports for any platform code that falls
     * outside the defined range.
     */
    UNKNOWN(0, "unknown"),

    /**
     * The Android mobile client.
     */
    ANDROID(1, "android"),

    /**
     * The iPhone mobile client.
     */
    IPHONE(2, "iphone"),

    /**
     * The Windows Phone client.
     */
    WP(3, "wp"),

    /**
     * The iOS tablet client.
     */
    IOS_TABLET(4, "ios_tablet"),

    /**
     * The KaiOS client.
     */
    KAIOS(5, "kaios"),

    /**
     * The native Windows desktop client.
     */
    WINDOWS(6, "windows"),

    /**
     * The Portal device client.
     */
    PORTAL(7, "portal"),

    /**
     * The Electron era macOS desktop client.
     */
    MAC_OS_ELECTRON(8, "mac_os_electron"),

    /**
     * The Electron era Windows desktop client.
     */
    WINDOWS_ELECTRON(9, "windows_electron"),

    /**
     * The wearable (watch) client.
     */
    WEARM(10, "wearm"),

    /**
     * The native macOS desktop client.
     */
    MACOS(11, "macos"),

    /**
     * The Cloud API client.
     */
    CAPI(12, "capi"),

    /**
     * The iPad client.
     */
    IPAD(13, "ipad"),

    /**
     * The Android business (SMB) client.
     */
    SMBA(14, "smba"),

    /**
     * The iOS business (SMB) client.
     */
    SMBI(15, "smbi"),

    /**
     * The web client.
     */
    WEB(16, "web");

    /**
     * Resolves an engine platform code to its platform, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * platform in constant time rather than by scanning {@link #values()}. A code with no entry falls back
     * to {@link #UNKNOWN} in {@link #ofCode(int)}.
     */
    private static final Map<Integer, CallParticipantPlatform> BY_CODE;

    /**
     * Resolves a wire token to its platform, backing {@link #ofToken(String)}.
     *
     * <p>Built once at class initialization from each constant's {@link #token}, so a token resolves to its
     * platform in constant time rather than by scanning {@link #values()}. Keys are the raw tokens, so
     * matching is case sensitive, preserving the {@link String#equals(Object)} semantics this lookup
     * replaces; an unrecognized or {@code null} token falls back to {@link #UNKNOWN} in
     * {@link #ofToken(String)}.
     */
    private static final Map<String, CallParticipantPlatform> BY_TOKEN;

    static {
        var byCode = new HashMap<Integer, CallParticipantPlatform>();
        var byToken = new HashMap<String, CallParticipantPlatform>();
        for (var platform : values()) {
            if (byCode.put(platform.code, platform) != null) {
                throw new AssertionError("Conflict");
            }
            if (byToken.put(platform.token, platform) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
        BY_TOKEN = Map.copyOf(byToken);
    }

    /**
     * The integer code the engine stores for this platform.
     */
    private final int code;

    /**
     * The lowercase wire token the engine emits for this platform.
     */
    private final String token;

    /**
     * Constructs a platform constant bound to its engine code and wire token.
     *
     * @param code  the integer code the engine stores
     * @param token the lowercase wire token the engine emits
     */
    CallParticipantPlatform(int code, String token) {
        this.code = code;
        this.token = token;
    }

    /**
     * Returns the integer code the engine stores for this platform.
     *
     * @return the engine platform code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the lowercase wire token the engine emits for this platform.
     *
     * @return the wire token, such as {@code "android"} or {@code "web"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the platform whose {@linkplain #code() code} equals the given value.
     *
     * <p>Any code outside the defined range {@code 0..16} resolves to {@link #UNKNOWN},
     * matching the engine's fallback for codes it does not recognize.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}.
     * @param code the engine platform code to resolve
     * @return the matching platform, or {@link #UNKNOWN} if the code is out of range
     */
    public static CallParticipantPlatform ofCode(int code) {
        var platform = BY_CODE.get(code);
        return platform != null ? platform : UNKNOWN;
    }

    /**
     * Returns the platform whose {@linkplain #token() wire token} equals the given token.
     *
     * <p>Any unrecognized or {@code null} token resolves to {@link #UNKNOWN}.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_TOKEN} map rather than
     * scanning {@link #values()}; a {@code null} token maps to no entry and falls back to {@link #UNKNOWN}.
     * @param token the wire token to resolve, may be {@code null}
     * @return the matching platform, or {@link #UNKNOWN} if the token is unrecognized
     */
    public static CallParticipantPlatform ofToken(String token) {
        var platform = BY_TOKEN.get(token);
        return platform != null ? platform : UNKNOWN;
    }
}
