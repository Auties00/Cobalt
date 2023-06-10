package it.auties.whatsapp.model.companion;

import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.Version;
import lombok.NonNull;

/**
 * A model for a mobile companion
 *
 * @param model the non-null model of the device
 * @param manufacturer the non-null manufacturer of the device
 * @param osType the non-null os of the device
 * @param osVersion the non-null os version of the device
 */
public record CompanionDevice(@NonNull String model, @NonNull String manufacturer, @NonNull UserAgentPlatform osType, @NonNull Version osVersion) {
    private static final CompanionDevice IPHONE_7 = new CompanionDevice("iPhone 7", "Apple", UserAgentPlatform.IOS, new Version("15.3.1"));
    private static final CompanionDevice SAMSUNG_GALAXY_S9 = new CompanionDevice("star2lte", "Samsung", UserAgentPlatform.ANDROID, new Version("8.0.0"));
    private static final CompanionDevice SURFACE_LAPTOP_STUDIO = new CompanionDevice("Surface Laptop Studio", "Microsoft", UserAgentPlatform.WINDOWS, new Version("10.0"));

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

    /**
     * Returns a Surface Laptop Studio
     *
     * @return a device
     */
    public static CompanionDevice windows() {
        return SURFACE_LAPTOP_STUDIO;
    }
}
