package com.github.auties00.cobalt.model.jid;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import com.github.auties00.cobalt.api.WhatsappClientType;
import com.github.auties00.cobalt.model.auth.UserAgent.PlatformType;
import com.github.auties00.cobalt.model.auth.Version;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ProtobufMessage
public final class JidDevice {
    private static final List<JidDevice> IOS_DEVICES = List.of(
            // --- iPhone 7 --- (Supports iOS 10-15)
            new JidDevice(
                    "iPhone 7",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone9,3",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 7",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone9,3",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone 7 Plus --- (Supports iOS 10-15)
            new JidDevice(
                    "iPhone 7 Plus",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone9,4",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 7 Plus",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone9,4",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone 8 --- (Supports iOS 11-16)
            new JidDevice(
                    "iPhone 8",
                    "Apple",
                    null,
                    null,
                    Version.of("13.7"),
                    "17H35",
                    "iPhone10,4",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 8",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,4",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 8",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,4",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 8",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,4",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone 8 Plus --- (Supports iOS 11-16)
            new JidDevice(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,5",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,5",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,5",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone X --- (Supports iOS 11-16)
            new JidDevice(
                    "iPhone X",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,6",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone X",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,6",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone X",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,6",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone XR --- (Supports iOS 12-17)
            new JidDevice(
                    "iPhone XR",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,8",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XR",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,8",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XR",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,8",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XR",
                    "Apple",
                    null,
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,8",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone XS --- (Supports iOS 12-17)
            new JidDevice(
                    "iPhone XS",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,2",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,2",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,2",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS",
                    "Apple",
                    null,
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,2",
                    WhatsappClientType.MOBILE
            ),

            // --- iPhone XS Max --- (Supports iOS 12-17)
            new JidDevice(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,6",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,6",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,6",
                    WhatsappClientType.MOBILE
            ),
            new JidDevice(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,6",
                    WhatsappClientType.MOBILE
            )
    );

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String model;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String manufacturer;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final PlatformType platform;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final Version appVersion;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final Version osVersion;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String osBuildNumber;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String modelId;

    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    final WhatsappClientType clientType;

    JidDevice(
            String model,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            String modelId,
            WhatsappClientType clientType
    ) {
        this.model = model;
        this.modelId = modelId;
        this.manufacturer = manufacturer;
        this.platform = platform;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.osBuildNumber = osBuildNumber;
        this.clientType = clientType;
    }

    public static JidDevice web() {
        return web(null);
    }

    public static JidDevice web(Version appVersion) {
        return new JidDevice(
                "Surface Pro 4",
                "Microsoft",
                PlatformType.MACOS,
                appVersion,
                Version.of("10.0"),
                null,
                null,
                WhatsappClientType.WEB
        );
    }

    public static JidDevice ios(boolean business) {
        return ios(null, business);
    }

    public static JidDevice ios(Version appVersion, boolean business) {
        var device = IOS_DEVICES.get(ThreadLocalRandom.current().nextInt(IOS_DEVICES.size()));
        return new JidDevice(
                device.model,
                device.manufacturer,
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                appVersion,
                device.osVersion,
                device.osBuildNumber,
                device.modelId,
                WhatsappClientType.MOBILE
        );
    }

    public static JidDevice android(boolean business) {
        return android(null, business);
    }

    public static JidDevice android(Version appVersion, boolean business) {
        var model = "Pixel_" + ThreadLocalRandom.current().nextInt(2, 9);
        return new JidDevice(
                model,
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                appVersion,
                Version.of(String.valueOf(ThreadLocalRandom.current().nextInt(11, 16))),
                null,
                model,
                WhatsappClientType.MOBILE
        );
    }

    public String osBuildNumber() {
        return Objects.requireNonNullElse(osBuildNumber, osVersion.toString());
    }

    public Optional<String> toUserAgent(Version appVersion) {
        var platformName = switch (platform()) {
            case ANDROID -> "Android";
            case ANDROID_BUSINESS -> "SMBA";
            case IOS -> "iOS";
            case IOS_BUSINESS -> "SMB iOS";
            default -> null;
        };
        if(platformName == null) {
            return Optional.empty();
        }

        var deviceName = switch (platform()) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer + "-" + model;
            case IOS, IOS_BUSINESS -> model;
            default -> null;
        };
        if(deviceName == null) {
            return Optional.empty();
        }

        var deviceVersion = osVersion.toString();
        return Optional.of("WhatsApp/%s %s/%s Device/%s".formatted(
                appVersion,
                platformName,
                deviceVersion,
                deviceName
        ));
    }

    public JidDevice toPersonal() {
        if (!platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toPersonal());
    }

    public JidDevice toBusiness() {
        if (platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toBusiness());
    }

    public JidDevice withPlatform(PlatformType platform) {
        return new JidDevice(
                model,
                manufacturer,
                Objects.requireNonNullElse(platform, this.platform),
                appVersion,
                osVersion,
                osBuildNumber,
                modelId,
                clientType
        );
    }

    public String model() {
        return model;
    }

    public String modelId() {
        return modelId;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public PlatformType platform() {
        return platform;
    }

    public Optional<Version> appVersion() {
        return Optional.ofNullable(appVersion);
    }

    public Version osVersion() {
        return osVersion;
    }

    public WhatsappClientType clientType() {
        return clientType;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JidDevice that
                && Objects.equals(model, that.model)
                && Objects.equals(manufacturer, that.manufacturer)
                && platform == that.platform
                && Objects.equals(appVersion, that.appVersion)
                && Objects.equals(osVersion, that.osVersion)
                && Objects.equals(osBuildNumber, that.osBuildNumber)
                && Objects.equals(modelId, that.modelId)
                && clientType == that.clientType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, manufacturer, platform, appVersion, osVersion, osBuildNumber, modelId, clientType);
    }

    @Override
    public String toString() {
        return "CompanionDevice[" +
                "model='" + model + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", platform=" + platform +
                ", appVersion=" + appVersion +
                ", osVersion=" + osVersion +
                ", osBuildNumber='" + osBuildNumber + '\'' +
                ", modelId='" + modelId + '\'' +
                ", clientType=" + clientType +
                ']';
    }
}
