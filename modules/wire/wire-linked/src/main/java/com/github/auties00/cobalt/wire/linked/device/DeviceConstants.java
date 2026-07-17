package com.github.auties00.cobalt.wire.linked.device;


/**
 * Device identifier constants used across WhatsApp's multi-device architecture.
 *
 * <p>Every WhatsApp account can be linked to a primary phone and up to four companion
 * devices (Web browsers, desktop apps, additional phones). Each linked device is
 * addressed by an integer identifier that is part of the device level JID. This class
 * centralises the well known, reserved device identifiers that Cobalt uses when
 * building or parsing these JIDs.
 *
 * @see DeviceListMetadata
 */
public final class DeviceConstants {

    /**
     * Prevents instantiation of this utility class.
     *
     * <p>This class exposes only static constants and must never be constructed.
     *
     * @throws AssertionError always
     */
    private DeviceConstants() {
        throw new AssertionError();
    }

    /**
     * Device identifier reserved for the primary device of an account.
     *
     * <p>The primary device is the phone that originally registered the WhatsApp
     * account. It owns the identity key and is the authority that authorises
     * companion devices via the ADV signed device identity protocol. Device id
     * {@code 0} is always interpreted as the primary device when parsing or
     * constructing device level JIDs.
     */
    public static final int PRIMARY_DEVICE_ID = 0;

    /**
     * Device identifier reserved for hosted (Business API) devices.
     *
     * <p>Hosted devices are server side companions that belong to a WhatsApp
     * Business account and are operated by a Business Solution Provider on behalf
     * of the account owner rather than by an end user device. They use the
     * reserved id {@code 99} so that other devices can recognise and route
     * messages to them correctly when the business coexistence feature is
     * enabled.
     */
    public static final int HOSTED_DEVICE_ID = 99;
}
