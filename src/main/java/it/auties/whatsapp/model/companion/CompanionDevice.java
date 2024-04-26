package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String modelId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String manufacturer,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        PlatformType platform,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<Version> appVersion,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Version osVersion,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String osBuildNumber,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> address
) implements ProtobufMessage {
    private static final List<Entry<String, String>> IPHONES = List.of(
            Map.entry("iPhone_7", "iPhone9,1"),
            Map.entry("iPhone_7_Plus", "iPhone9,2"),
            Map.entry("iPhone_8", "iPhone10,1"),
            Map.entry("iPhone_8_Plus", "iPhone10,2"),
            Map.entry("iPhone_X", "iPhone10,3"),
            Map.entry("iPhone_XR", "iPhone11,8"),
            Map.entry("iPhone_XS", "iPhone11,2"),
            Map.entry("iPhone_XS_Max", "iPhone11,6")
    );
    private static final List<Entry<String, String>> IOS_VERSION = List.of(
            Map.entry("16.7", "20H19"),
            Map.entry("16.7.1", "20H30"),
            Map.entry("16.7.2", "20H115"),
            Map.entry("16.7.3", "20H232"),
            Map.entry("16.7.4", "20H240"),
            Map.entry("16.7.5", "20H307"),
            Map.entry("16.7.6", "20H320"),
            Map.entry("16.7.7", "20H330")
    );

    public static CompanionDevice web() {
        return web(null);
    }

    public static CompanionDevice web(Version appVersion) {
        return new CompanionDevice(
                "Chrome",
                "Chrome",
                "Google",
                PlatformType.WEB,
                Optional.ofNullable(appVersion),
                Version.of("1.0"),
                null,
                Optional.empty()
        );
    }

    public static CompanionDevice ios(boolean business) {
        return ios(null, business);
    }

    public static CompanionDevice ios(Version appVersion, boolean business) {
        var model = IPHONES.get(ThreadLocalRandom.current().nextInt(IPHONES.size()));
        var version = IOS_VERSION.get(ThreadLocalRandom.current().nextInt(IOS_VERSION.size()));
        return new CompanionDevice(
                model.getKey(),
                model.getValue(),
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                Optional.ofNullable(appVersion),
                Version.of(version.getKey()),
                version.getValue(),
                Optional.empty()
        );
    }

    public static CompanionDevice android(boolean business) {
        return android(null, business, null);
    }

    public static CompanionDevice android(boolean business, String deviceAddress) {
        return android(null, business, deviceAddress);
    }

    public static CompanionDevice android(Version appVersion, boolean business, String deviceAddress) {
        return new CompanionDevice(
                "Pixel_2",
                "Pixel_2",
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                Optional.ofNullable(appVersion),
                Version.of("11"),
                null,
                Optional.ofNullable(deviceAddress)
        );
    }

    public static CompanionDevice kaiOs() {
        return kaiOs(null);
    }

    public static CompanionDevice kaiOs(Version appVersion) {
        return new CompanionDevice(
                "8110",
                "8110",
                "Nokia",
                PlatformType.KAIOS,
                Optional.ofNullable(appVersion),
                Version.of("2.5.4"),
                null,
                Optional.empty()
        );
    }

    @Override
    public String osBuildNumber() {
        return Objects.requireNonNullElse(osBuildNumber, osVersion.toString());
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
                modelId,
                manufacturer,
                platform.toPersonal(),
                appVersion,
                osVersion,
                osBuildNumber,
                address
        );
    }

    public CompanionDevice toBusiness() {
        if(platform.isBusiness()) {
            return this;
        }

        return new CompanionDevice(
                model,
                modelId,
                manufacturer,
                platform.toBusiness(),
                appVersion,
                osVersion,
                osBuildNumber,
                address
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
