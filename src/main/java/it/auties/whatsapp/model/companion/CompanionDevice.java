package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

/**
 * A model for a mobile companion
 */
@ProtobufMessage
public final class CompanionDevice {
    private static final List<Entry<String, String>> IOS_VERSION = List.of(
            Map.entry("17.5.1", "21F91")
    );

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String model;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String manufacturer;

    @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
    private final PlatformType platform;

    @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
    private final Version appVersion;

    @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
    private final Version osVersion;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String osBuildNumber;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private final String modelId;

    @ProtobufProperty(index = 9, type = ProtobufType.OBJECT)
    private final ClientType clientType;

    CompanionDevice(
            String model,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            String modelId,
            ClientType clientType
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

    public static CompanionDevice web() {
        return web(null);
    }

    public static CompanionDevice web(Version appVersion) {
        return new CompanionDevice(
                "Chrome",
                "Google",
                PlatformType.WEB,
                appVersion,
                Version.of("1.0"),
                null,
                null,
                ClientType.WEB
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
                appVersion,
                Version.of("17.2.1"),
                "",
                "iPhone16,2",
                ClientType.MOBILE
        );
    }

    public static CompanionDevice android(boolean business) {
        return android(null, business);
    }

    public static CompanionDevice android(Version appVersion, boolean business) {
        return new CompanionDevice(
                "Pixel_4",
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                appVersion,
                Version.of("12"),
                null,
                "Pixel_4",
                ClientType.MOBILE
        );
    }

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
        if (!platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toPersonal());
    }

    public CompanionDevice toBusiness() {
        if (platform.isBusiness()) {
            return this;
        }

        return withPlatform(platform.toBusiness());
    }

    public CompanionDevice withPlatform(PlatformType platform) {
        return new CompanionDevice(
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

    private String deviceVersion() {
        return osVersion.toString();
    }

    private String deviceName() {
        return switch (platform()) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer + "-" + model;
            case IOS, IOS_BUSINESS -> model;
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private String platformName() {
        return switch (platform()) {
            case ANDROID -> "Android";
            case ANDROID_BUSINESS -> "SMBA";
            case IOS -> "iOS";
            case IOS_BUSINESS -> "SMB iOS";
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
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

    public ClientType clientType() {
        return clientType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanionDevice that = (CompanionDevice) o;
        return Objects.equals(model, that.model) && Objects.equals(manufacturer, that.manufacturer) && platform == that.platform && Objects.equals(appVersion, that.appVersion) && Objects.equals(osVersion, that.osVersion) && Objects.equals(osBuildNumber, that.osBuildNumber) && Objects.equals(modelId, that.modelId) && clientType == that.clientType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, manufacturer, platform, appVersion, osVersion, osBuildNumber, modelId, clientType);
    }

    @Override
    public String toString() {
        return "CompanionDevice{" +
                "model='" + model + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", platform=" + platform +
                ", appVersion=" + appVersion +
                ", osVersion=" + osVersion +
                ", osBuildNumber='" + osBuildNumber + '\'' +
                ", modelId='" + modelId + '\'' +
                ", clientType=" + clientType +
                '}';
    }
}
