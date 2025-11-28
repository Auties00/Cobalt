package com.github.auties00.cobalt.model.jid;

import com.github.auties00.cobalt.client.WhatsAppClientType;
import com.github.auties00.cobalt.model.auth.UserAgent.PlatformType;
import com.github.auties00.cobalt.model.auth.Version;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@ProtobufMessage
public final class JidCompanion {
    private static final List<JidCompanion> IOS_DEVICES = List.of(
            // --- iPhone 7 --- (Supports iOS 10-15)
            new JidCompanion(
                    "iPhone 7",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone9,3",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 7",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone9,3",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone 7 Plus --- (Supports iOS 10-15)
            new JidCompanion(
                    "iPhone 7 Plus",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone9,4",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 7 Plus",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone9,4",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone 8 --- (Supports iOS 11-16)
            new JidCompanion(
                    "iPhone 8",
                    "Apple",
                    null,
                    Version.of("13.7"),
                    "17H35",
                    "iPhone10,4",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 8",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,4",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 8",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,4",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 8",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,4",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone 8 Plus --- (Supports iOS 11-16)
            new JidCompanion(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,5",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,5",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone 8 Plus",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,5",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone X --- (Supports iOS 11-16)
            new JidCompanion(
                    "iPhone X",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone10,6",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone X",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone10,6",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone X",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone10,6",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone XR --- (Supports iOS 12-17)
            new JidCompanion(
                    "iPhone XR",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,8",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XR",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,8",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XR",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,8",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XR",
                    "Apple",
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,8",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone XS --- (Supports iOS 12-17)
            new JidCompanion(
                    "iPhone XS",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,2",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,2",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,2",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS",
                    "Apple",
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,2",
                    WhatsAppClientType.MOBILE
            ),

            // --- iPhone XS Max --- (Supports iOS 12-17)
            new JidCompanion(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    Version.of("14.8.1"),
                    "18H107",
                    "iPhone11,6",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    Version.of("15.8.2"),
                    "19H384",
                    "iPhone11,6",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    Version.of("16.7.7"),
                    "20H330",
                    "iPhone11,6",
                    WhatsAppClientType.MOBILE
            ),
            new JidCompanion(
                    "iPhone XS Max",
                    "Apple",
                    null,
                    Version.of("17.4.1"),
                    "21E236",
                    "iPhone11,6",
                    WhatsAppClientType.MOBILE
            )
    );

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String model;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String manufacturer;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final PlatformType platform;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final Version osVersion;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String osBuildNumber;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String modelId;

    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final WhatsAppClientType clientType;

    JidCompanion(
            String model,
            String manufacturer,
            PlatformType platform,
            Version osVersion,
            String osBuildNumber,
            String modelId,
            WhatsAppClientType clientType
    ) {
        this.model = model;
        this.modelId = modelId;
        this.manufacturer = manufacturer;
        this.platform = platform;
        this.osVersion = osVersion;
        this.osBuildNumber = osBuildNumber;
        this.clientType = clientType;
    }

    public static JidCompanion web() {
        return new JidCompanion(
                "Surface Pro 4",
                "Microsoft",
                PlatformType.MACOS,
                Version.of("10.0"),
                null,
                null,
                WhatsAppClientType.WEB
        );
    }

    public static JidCompanion ios(boolean business) {
        var device = IOS_DEVICES.get(ThreadLocalRandom.current().nextInt(IOS_DEVICES.size()));
        return new JidCompanion(
                device.model,
                device.manufacturer,
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                device.osVersion,
                device.osBuildNumber,
                device.modelId,
                WhatsAppClientType.MOBILE
        );
    }

    public static JidCompanion android(boolean business) {
        var model = "Pixel_" + ThreadLocalRandom.current().nextInt(2, 9);
        return new JidCompanion(
                model,
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                Version.of(String.valueOf(ThreadLocalRandom.current().nextInt(11, 16))),
                null,
                model,
                WhatsAppClientType.MOBILE
        );
    }

    public String osBuildNumber() {
        return Objects.requireNonNullElse(osBuildNumber, osVersion.toString());
    }

    public String toUserAgent(Version clientVersion) {
        if(platform == PlatformType.WINDOWS || platform == PlatformType.MACOS) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
        }else {
            var platformName = switch (platform) {
                case ANDROID -> "Android";
                case ANDROID_BUSINESS -> "SMBA";
                case IOS -> "iOS";
                case IOS_BUSINESS -> "SMB iOS";
                case MACOS, WINDOWS -> throw new InternalError();
            };
            var deviceName = switch (platform()) {
                case ANDROID, ANDROID_BUSINESS -> manufacturer + "-" + model;
                case IOS, IOS_BUSINESS -> model;
                case MACOS, WINDOWS -> throw new InternalError();
            };
            var deviceVersion = osVersion.toString();
            return "WhatsApp/%s %s/%s Device/%s".formatted(
                    clientVersion,
                    platformName,
                    deviceVersion,
                    deviceName
            );
        }
    }

    public JidCompanion toPersonal() {
        if (!platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toPersonal());
    }

    public JidCompanion toBusiness() {
        if (platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toBusiness());
    }

    public JidCompanion withPlatform(PlatformType platform) {
        return new JidCompanion(
                model,
                manufacturer,
                Objects.requireNonNullElse(platform, this.platform),
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

    public Version osVersion() {
        return osVersion;
    }

    public WhatsAppClientType clientType() {
        return clientType;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JidCompanion that
                && Objects.equals(model, that.model)
                && Objects.equals(manufacturer, that.manufacturer)
                && platform == that.platform
                && Objects.equals(osVersion, that.osVersion)
                && Objects.equals(osBuildNumber, that.osBuildNumber)
                && Objects.equals(modelId, that.modelId)
                && clientType == that.clientType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, manufacturer, platform, osVersion, osBuildNumber, modelId, clientType);
    }

    @Override
    public String toString() {
        return "JidCompanion[" +
                "model='" + model + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", platform=" + platform +
                ", osVersion=" + osVersion +
                ", osBuildNumber='" + osBuildNumber + '\'' +
                ", modelId='" + modelId + '\'' +
                ", clientType=" + clientType +
                ']';
    }
}
