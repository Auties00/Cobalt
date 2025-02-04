package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A model for a mobile companion
 */
@ProtobufMessage
public final class CompanionDevice {
    private static final List<Entry<String, String>> IPHONES = List.of(
            Map.entry("iPhone_7", "iPhone12,1")
    );
    private static final List<Entry<String, String>> IOS_VERSION = List.of(
            Map.entry("15.8.3", "21G101")
    );
    private static final int MIDDLEWARE_BUSINESS_PORT = 1120;
    private static final int MIDDLEWARE_PERSONAL_PORT = 1119;

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String model;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String manufacturer;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    private final PlatformType platform;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final Version appVersion;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final Version osVersion;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String osBuildNumber;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String address;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private final String modelId;

    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    private final ClientType clientType;

    private CompanionDevice(
            String model,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            List<String> addresses,
            String modelId,
            ClientType clientType
    ) {
        this(
                model,
                manufacturer,
                platform,
                appVersion,
                osVersion,
                osBuildNumber,
                addresses == null || addresses.isEmpty() ? null : addresses.get(ThreadLocalRandom.current().nextInt(0, addresses.size())),
                modelId,
                clientType
        );
    }

    CompanionDevice(
            String model,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            String address,
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
        var uri = address == null ? null : URI.create(address);
        this.address = uri == null ? null : "%s://%s:%s".formatted(
                Objects.requireNonNullElse(uri.getScheme(), "http"),
                Objects.requireNonNull(uri.getHost(), "Missing hostname"),
                uri.getPort() != -1 ? uri.getPort() : (platform.isBusiness() ? MIDDLEWARE_BUSINESS_PORT : MIDDLEWARE_PERSONAL_PORT)
        );
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
                List.of(),
                null,
                ClientType.WEB
        );
    }

    public static CompanionDevice ios(boolean business) {
        return ios(null, business, null);
    }

    public static CompanionDevice ios(boolean business, String address) {
        return ios(null, business, address == null ? null : List.of(address));
    }

    public static CompanionDevice ios(boolean business, List<String> address) {
        return ios(null, business, address);
    }

    public static CompanionDevice ios(Version appVersion, boolean business, List<String> address) {
        var model = IPHONES.get(ThreadLocalRandom.current().nextInt(IPHONES.size()));
        var version = IOS_VERSION.get(ThreadLocalRandom.current().nextInt(IOS_VERSION.size()));
        return new CompanionDevice(
                model.getKey(),
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                appVersion,
                Version.of(version.getKey()),
                version.getValue(),
                address,
                model.getValue(),
                ClientType.MOBILE
        );
    }

    public static CompanionDevice android(boolean business) {
        return android(null, business, null);
    }

    public static CompanionDevice android(boolean business, String address) {
        return android(null, business, address == null ? null : List.of(address));
    }

    public static CompanionDevice android(boolean business, List<String> address) {
        return android(null, business, address);
    }

    public static CompanionDevice android(Version appVersion, boolean business, List<String> address) {
        var model = "Pixel_" + ThreadLocalRandom.current().nextInt(2, 9);
        return new CompanionDevice(
                model,
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                appVersion,
                Version.of(String.valueOf(ThreadLocalRandom.current().nextInt(11, 16))),
                null,
                address,
                model,
                ClientType.MOBILE
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
                appVersion,
                Version.of("2.5.4"),
                null,
                List.of(),
                "8110",
                ClientType.MOBILE
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
            case KAIOS -> "KaiOS";
            default -> null;
        };
        if(platformName == null) {
            return Optional.empty();
        }

        var deviceName = switch (platform()) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer + "-" + model;
            case IOS, IOS_BUSINESS -> model;
            case KAIOS -> manufacturer + "+" + model;
            default -> null;
        };
        if(deviceName == null) {
            return Optional.empty();
        }

        var deviceVersion = platform.isKaiOs() ? "%s+20190925153113".formatted(osVersion) : osVersion.toString();
        return Optional.of("WhatsApp/%s %s/%s Device/%s".formatted(
                appVersion,
                platformName,
                deviceVersion,
                deviceName
        ));
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
                address,
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

    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    public ClientType clientType() {
        return clientType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanionDevice that = (CompanionDevice) o;
        return Objects.equals(model, that.model) && Objects.equals(manufacturer, that.manufacturer) && platform == that.platform && Objects.equals(appVersion, that.appVersion) && Objects.equals(osVersion, that.osVersion) && Objects.equals(osBuildNumber, that.osBuildNumber) && Objects.equals(address, that.address) && Objects.equals(modelId, that.modelId) && clientType == that.clientType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, manufacturer, platform, appVersion, osVersion, osBuildNumber, address, modelId, clientType);
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
                ", address='" + address + '\'' +
                ", modelId='" + modelId + '\'' +
                ", clientType=" + clientType +
                '}';
    }
}
