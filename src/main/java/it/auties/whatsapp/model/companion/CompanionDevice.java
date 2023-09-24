package it.auties.whatsapp.model.companion;

import it.auties.whatsapp.model.signal.auth.UserAgent.Platform;
import it.auties.whatsapp.model.signal.auth.Version;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model for a mobile companion
 *
 * @param model the non-null model of the device
 * @param manufacturer the non-null manufacturer of the device
 * @param platform the non-null os of the device
 * @param osVersion the non-null os version of the device
 */
public record CompanionDevice(@NonNull String model, @NonNull String manufacturer, @NonNull Platform platform, @NonNull Platform businessPlatform, @NonNull Version osVersion) {
    private static final CompanionDevice IPHONE_7 = new CompanionDevice("iPhone 7", "Apple", Platform.IOS, Platform.SMB_IOS, Version.of("15.3.1"));
    private static final CompanionDevice SAMSUNG_GALAXY_S9 = new CompanionDevice("star2lte", "Samsung", Platform.ANDROID, Platform.SMB_ANDROID, Version.of("8.0.0"));

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
