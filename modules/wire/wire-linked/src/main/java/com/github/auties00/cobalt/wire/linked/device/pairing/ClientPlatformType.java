package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Identifies the platform that a WhatsApp client is running on.
 *
 * <p>The value is sent to WhatsApp's servers as part of the user agent carried inside the
 * {@link ClientPayload} during the handshake. The server uses it to branch feature
 * availability, enforce per platform limits, format some responses differently and track
 * device linking stats. The enum covers every platform that WhatsApp and related apps in
 * the Meta family of products have ever shipped, from the original consumer mobile apps
 * to more recent surfaces such as VR headsets and smart glasses.
 *
 * <p>Values whose names start with {@code BLUE_} or {@code GREEN_} refer to Messenger
 * variants that share infrastructure with WhatsApp, while {@code IGLITE_ANDROID},
 * {@code FBLITE_ANDROID} and {@code MLITE_ANDROID} identify the low end lite builds of
 * the corresponding apps. Most Cobalt users only need {@link #WEB}, {@link #WINDOWS} or
 * {@link #MACOS}, matching the three environments the library can impersonate.
 */
@ProtobufEnum(name = "ClientPayload.UserAgent.Platform")
public enum ClientPlatformType {
    /** Standard consumer WhatsApp on Android phones. */
    ANDROID(0),
    /** Standard consumer WhatsApp on iPhone. */
    IOS(1),
    /** WhatsApp on Windows Phone, long deprecated. */
    WINDOWS_PHONE(2),
    /** Legacy BlackBerry OS build. */
    BLACKBERRY(3),
    /** BlackBerry 10 (formerly BBX) build. */
    BLACKBERRYX(4),
    /** Nokia Series 40 feature phone build. */
    S40(5),
    /** Nokia Series 60 smartphone build. */
    S60(6),
    /** Python reference client, historically used for internal tooling and testing. */
    PYTHON_CLIENT(7),
    /** Samsung Tizen build. */
    TIZEN(8),
    /** WhatsApp Enterprise client. */
    ENTERPRISE(9),
    /** WhatsApp Business on Android. */
    ANDROID_BUSINESS(10),
    /** KaiOS feature phone build. */
    KAIOS(11),
    /** WhatsApp Business on iPhone. */
    IOS_BUSINESS(12),
    /** WhatsApp Desktop on Windows. */
    WINDOWS(13),
    /** WhatsApp Web running in a browser tab. */
    WEB(14),
    /** WhatsApp on the Facebook Portal family of devices. */
    PORTAL(15),
    /** Messenger variant on Android (internally codenamed green). */
    GREEN_ANDROID(16),
    /** Messenger variant on iPhone (internally codenamed green). */
    GREEN_IPHONE(17),
    /** Messenger variant on Android running alongside WhatsApp (codenamed blue). */
    BLUE_ANDROID(18),
    /** Messenger variant on iPhone running alongside WhatsApp (codenamed blue). */
    BLUE_IPHONE(19),
    /** Facebook Lite on Android. */
    FBLITE_ANDROID(20),
    /** Messenger Lite on Android. */
    MLITE_ANDROID(21),
    /** Instagram Lite on Android. */
    IGLITE_ANDROID(22),
    /** Facebook Pages client. */
    PAGE(23),
    /** WhatsApp Desktop on macOS. */
    MACOS(24),
    /** Oculus Messaging surface. */
    OCULUS_MSG(25),
    /** Oculus Calling surface. */
    OCULUS_CALL(26),
    /** Milan platform build. */
    MILAN(27),
    /** CAPI (Conversations API) client, used by business integrations. */
    CAPI(28),
    /** Wear OS companion build for smartwatches. */
    WEAROS(29),
    /** Augmented reality device build. */
    ARDEVICE(30),
    /** Virtual reality device build. */
    VRDEVICE(31),
    /** Messenger web surface (codenamed blue). */
    BLUE_WEB(32),
    /** WhatsApp on iPad. */
    IPAD(33),
    /** Internal test platform used by WhatsApp engineering. */
    TEST(34),
    /** Smart glasses build. */
    SMART_GLASSES(35),
    /** Messenger variant on VR headsets (codenamed blue). */
    BLUE_VR(36),
    /** AR wrist worn device build. */
    AR_WRIST(37);

    /**
     * Protobuf constructor that records the numeric wire value assigned to each entry.
     *
     * @param index the stable wire index as defined by WhatsApp's protobuf schema
     */
    ClientPlatformType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Stable wire value of this entry, matching the corresponding WhatsApp protobuf enum.
     */
    final int index;

    /**
     * Returns the wire value of this platform entry.
     *
     * @return the numeric index that WhatsApp's servers use to identify this platform
     */
    public int index() {
        return this.index;
    }

    /**
     * Returns whether this platform is one of the web environments Cobalt can impersonate.
     *
     * @return {@code true} for {@link #WEB}, {@code false} otherwise
     */
    public boolean isWeb() {
        return this == WEB;
    }

    /**
     * Returns whether this platform is one of the mobile environments Cobalt can impersonate.
     *
     * @return {@code true} for {@link #ANDROID}, {@link #ANDROID_BUSINESS}, {@link #IOS}, {@link #IOS_BUSINESS}, {@code false} otherwise
     */
    public boolean isMobile() {
        return this == ANDROID || this == ANDROID_BUSINESS || this == IOS || this == IOS_BUSINESS;
    }

    /**
     * Returns whether this platform is one of the desktop environments Cobalt can impersonate.
     *
     * @return {@code true} for {@link #WINDOWS} and {@link #MACOS}, {@code false} otherwise
     */
    public boolean isDesktop() {
        return this == WINDOWS || this == MACOS;
    }
}
