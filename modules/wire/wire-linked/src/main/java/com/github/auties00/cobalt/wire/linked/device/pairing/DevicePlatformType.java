package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumeration of the device platform families recognised by WhatsApp when a
 * companion device registers with the account.
 *
 * <p>When Cobalt or any other WhatsApp client completes the pairing flow with
 * the primary device, it must advertise which kind of device it is running on
 * so that the server can tailor feature availability, push notification
 * routing, telemetry and UI behaviour. The platform advertised here is
 * included in {@link com.github.auties00.cobalt.wire.linked.device.DeviceProps} and is surfaced inside the WhatsApp app on
 * the "Linked devices" screen (as the small icon and label shown next to each
 * companion).
 *
 * <p>The platform types cover browsers, desktop applications, mobile tablets
 * and phones, wearables, virtual reality headsets, smart glasses and the
 * server side Cloud API. {@link #UNKNOWN} is used as a safe default when the
 * platform cannot be determined.
 *
 * @see com.github.auties00.cobalt.wire.linked.device.DeviceProps
 */
@ProtobufEnum(name = "DeviceProps.PlatformType")
public enum DevicePlatformType {
    /**
     * Unknown or unspecified platform. Used when the client cannot determine
     * the runtime environment or does not wish to disclose it.
     */
    UNKNOWN(0),
    /**
     * Google Chrome browser running the WhatsApp Web client.
     */
    CHROME(1),
    /**
     * Mozilla Firefox browser running the WhatsApp Web client.
     */
    FIREFOX(2),
    /**
     * Microsoft Internet Explorer browser running the WhatsApp Web client.
     */
    IE(3),
    /**
     * Opera browser running the WhatsApp Web client.
     */
    OPERA(4),
    /**
     * Apple Safari browser running the WhatsApp Web client.
     */
    SAFARI(5),
    /**
     * Microsoft Edge browser running the WhatsApp Web client.
     */
    EDGE(6),
    /**
     * Native desktop application (WhatsApp for Windows, WhatsApp for macOS or
     * the older Electron based desktop client).
     */
    DESKTOP(7),
    /**
     * Apple iPad tablet running the native WhatsApp for iPad application.
     */
    IPAD(8),
    /**
     * Android tablet running the native WhatsApp application as a companion
     * device.
     */
    ANDROID_TABLET(9),
    /**
     * Ohana platform (KaiOS feature phone).
     */
    OHANA(10),
    /**
     * Aloha platform (Meta Portal smart display).
     */
    ALOHA(11),
    /**
     * Catalina platform (Apple's iPad to Mac framework used by the older
     * WhatsApp for Mac client).
     */
    CATALINA(12),
    /**
     * TCL branded smart television running a native WhatsApp client.
     */
    TCL_TV(13),
    /**
     * Apple iPhone running the native WhatsApp for iOS application as the
     * primary device.
     */
    IOS_PHONE(14),
    /**
     * Mac Catalyst build of the iOS application running on macOS.
     */
    IOS_CATALYST(15),
    /**
     * Android phone running the native WhatsApp for Android application as
     * the primary device.
     */
    ANDROID_PHONE(16),
    /**
     * Android device whose form factor (phone or tablet) could not be
     * disambiguated by the server.
     */
    ANDROID_AMBIGUOUS(17),
    /**
     * Wear OS smartwatch running the native WhatsApp companion.
     */
    WEAR_OS(18),
    /**
     * Meta augmented reality wrist device.
     */
    AR_WRIST(19),
    /**
     * Meta augmented reality headset or glasses (general AR device category).
     */
    AR_DEVICE(20),
    /**
     * Universal Windows Platform application (the Microsoft Store WhatsApp
     * build prior to the Electron native desktop client).
     */
    UWP(21),
    /**
     * Virtual reality headset (Meta Quest family).
     */
    VR(22),
    /**
     * WhatsApp Business Cloud API hosted device, operated server side by a
     * Business Solution Provider rather than running on an end user device.
     */
    CLOUD_API(23),
    /**
     * Smart glasses (for example Ray Ban Meta) running the native WhatsApp
     * companion.
     */
    SMARTGLASSES(24);

    /**
     * Creates a new platform constant with the given protobuf wire index.
     *
     * @param index the protobuf enum index bound to this constant
     */
    DevicePlatformType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index of this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire index associated with this platform constant.
     *
     * <p>The returned value is the numeric identifier transmitted on the wire
     * when the enum is serialised. It matches the values defined in the
     * WhatsApp {@code DeviceProps.PlatformType} protobuf schema.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return this.index;
    }
}
