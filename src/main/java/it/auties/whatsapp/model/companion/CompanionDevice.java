package it.auties.whatsapp.model.companion;

import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

/**
 * A model for a mobile companion
 *
 * @param model        the non-null model of the device
 * @param manufacturer the non-null manufacturer of the device
 * @param platform     the non-null os of the device
 * @param osVersion    the non-null os version of the device
 */
public record CompanionDevice(String model, String manufacturer, PlatformType platform, PlatformType businessPlatform,
                              Version osVersion) {
    private static final CompanionDevice IPHONE_7 = new CompanionDevice("iPhone 7", "Apple", PlatformType.IOS, PlatformType.SMB_IOS, Version.of("15.3.1"));
    private static final CompanionDevice SAMSUNG_GALAXY_S9 = new CompanionDevice("star2lte", "Samsung", PlatformType.ANDROID, PlatformType.SMB_ANDROID, Version.of("8.0.0"));

    /**
     * Returns an Iphone 7
     *
     * @return a device
     */
    public static CompanionDevice ios() {
        return IPHONE_7;
    }

    /**
     * Returns an Samsung Galaxy S9
     *
     * @return a device
     */
    public static CompanionDevice android() {
        return SAMSUNG_GALAXY_S9;
    }
}
