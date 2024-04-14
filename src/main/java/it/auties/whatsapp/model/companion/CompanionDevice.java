package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

import java.util.Optional;

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
                "iPhone_15_Pro_Max",
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                Optional.ofNullable(appVersion),
                Version.of("17.4.1")
        );
    }

    public String toUserAgent(Version appVersion) {
        return "WhatsApp/%s %s/%s Device/%s".formatted(
                appVersion,
                platformName(),
                osVersion(),
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

    private String deviceName() {
        return switch (platform()) {
            case IOS, IOS_BUSINESS -> model;
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private String platformName() {
        return switch (platform()) {
            case IOS -> "iOS";
            case IOS_BUSINESS -> "SMB iOS";
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }
}
