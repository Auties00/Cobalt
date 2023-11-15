package it.auties.whatsapp.model.companion;

import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

/**
 * A model for a mobile companion
 *
 * @param model        the non-null model of the device
 * @param manufacturer the non-null manufacturer of the device
 * @param platform     the non-null os of the device
 * @param version    the non-null os version of the device
 */
public record CompanionDevice(String model, String manufacturer, PlatformType platform, String version) {
    public static CompanionDevice web() {
        return new CompanionDevice("Chrome", "Google", PlatformType.WEB,"1.0");
    }

    public static CompanionDevice ios(boolean business) {
        return new CompanionDevice(
                "iPhone_X",
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                "16.5.1"
        );
    }

    public static CompanionDevice android(boolean business) {
        return new CompanionDevice(
                "P60",
                "HUAWEI",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                "10.11.0"
        );
    }

    public static CompanionDevice kaiOs() {
        return new CompanionDevice(
                "8110",
                "Nokia",
                PlatformType.KAIOS,
               "2.5.4"
        );
    }

    public String toUserAgent(Version appVersion) {
        return "WhatsApp/%s %s/%s Device/%s".formatted(
                appVersion,
                platformName(),
                deviceVersion(),
                deviceName()
        );
    }

    private String deviceVersion() {
        if(platform.isKaiOs()) {
            return "%s+20190925153113".formatted(version);
        }

        return version;
    }

    private String deviceName() {
        return switch (platform()) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer + " " + model;
            case IOS, IOS_BUSINESS -> model;
            case KAIOS -> manufacturer + "+" + model;
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private String platformName() {
        return switch (platform()) {
            case ANDROID -> "Android";
            case ANDROID_BUSINESS -> "SMBA";
            case IOS -> "iOS";
            case IOS_BUSINESS -> "SMB iOS";
            case KAIOS -> "KaiOS";
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }
}
