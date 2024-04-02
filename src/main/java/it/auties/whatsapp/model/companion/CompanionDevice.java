package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A model for a mobile companion
 *
 * @param model        the non-null model of the device
 * @param manufacturer the non-null manufacturer of the device
 * @param platform     the non-null os of the device
 * @param appVersion   the version of the app, or empty
 * @param osVersion    the non-null os version of the device
 */
public record CompanionDevice(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String model,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String manufacturer,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        PlatformType platform,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<Version> appVersion,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Version osVersion
) implements ProtobufMessage {
    private static final List<String> IPHONES = List.of(
            "iPhone_11",
            "iPhone_11_Pro",
            "iPhone_11_Pro_Max",
            "iPhone_12",
            "iPhone_12_Pro",
            "iPhone_12_Pro_Max",
            "iPhone_13",
            "iPhone_13_Pro",
            "iPhone_13_Pro_Max",
            "iPhone_14",
            "iPhone_14_Plus",
            "iPhone_14_Pro",
            "iPhone_14_Pro_Max",
            "iPhone_15",
            "iPhone_15_Plus",
            "iPhone_15_Pro",
            "iPhone_15_Pro_Max"
    );

    public static CompanionDevice web() {
        return web(null);
    }

    public static CompanionDevice web(Version appVersion) {
        return new CompanionDevice(
                "Chrome",
                "Google",
                PlatformType.WEB,
                Optional.ofNullable(appVersion),
                Version.of("1.0")
        );
    }

    public static CompanionDevice ios(boolean business) {
        return ios(null, business);
    }

    public static CompanionDevice ios(Version appVersion, boolean business) {
        return new CompanionDevice(
                IPHONES.get(ThreadLocalRandom.current().nextInt(IPHONES.size())),
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                Optional.ofNullable(appVersion),
                Version.of("17.3.1")
        );
    }

    public static CompanionDevice android(boolean business) {
        return android(null, business);
    }


    public static CompanionDevice android(Version appVersion, boolean business) {
        return new CompanionDevice(
                "Pixel_2",
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                Optional.ofNullable(appVersion),
                Version.of("11")
        );
    }

    public static CompanionDevice kaiOs() {
        return kaiOs(null);
    }

    public static CompanionDevice kaiOs(Version appVersion) {
        return new CompanionDevice(
                "8110",
                "Nokia",
                PlatformType.KAIOS,
                Optional.ofNullable(appVersion),
                Version.of("2.5.4")
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

    public CompanionDevice toPersonal() {
        if(!platform.isBusiness()) {
            return this;
        }

        return new CompanionDevice(
                model,
                manufacturer,
                platform.toPersonal(),
                appVersion,
                osVersion
        );
    }

    public CompanionDevice toBusiness() {
        if(platform.isBusiness()) {
            return this;
        }

        return new CompanionDevice(
                model,
                manufacturer,
                platform.toBusiness(),
                appVersion,
                osVersion
        );
    }

    private String deviceVersion() {
        return platform.isKaiOs() ? "%s+20190925153113".formatted(osVersion) : osVersion.toString();
    }

    private String deviceName() {
        return switch (platform()) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer + "-" + model;
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
