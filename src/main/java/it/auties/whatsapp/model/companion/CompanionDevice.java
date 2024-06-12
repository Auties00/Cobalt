package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
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
public final class CompanionDevice implements ProtobufMessage {
    private static final List<Entry<String, String>> IPHONES = List.of(
            Map.entry("iPhone_11", "iPhone12,1"),
            Map.entry("iPhone_11_Pro", "iPhone12,3"),
            Map.entry("iPhone_11_Pro_Max", "iPhone12,5"),
            Map.entry("iPhone_12", "iPhone13,2"),
            Map.entry("iPhone_12_Pro", "iPhone13,3"),
            Map.entry("iPhone_12_Pro_Max", "iPhone13,4"),
            Map.entry("iPhone_13", "iPhone14,5"),
            Map.entry("iPhone_13_Pro", "iPhone14,2"),
            Map.entry("iPhone_13_Pro_Max", "iPhone14,3"),
            Map.entry("iPhone_14", "iPhone14,7"),
            Map.entry("iPhone_14_Plus", "iPhone14,8"),
            Map.entry("iPhone_14_Pro", "iPhone15,2"),
            Map.entry("iPhone_14_Pro_Max", "iPhone15,3"),
            Map.entry("iPhone_15", "iPhone15,4"),
            Map.entry("iPhone_15_Plus", "iPhone15,5"),
            Map.entry("iPhone_15_Pro", "iPhone16,1"),
            Map.entry("iPhone_15_Pro_Max", "iPhone16,2"),
            Map.entry("iPhone_X", "iPhone12,1")
    );
    private static final List<String> IOS_VERSION = List.of(
            "17.0",
            "17.0.1",
            "17.0.2",
            "17.0.3",
            "17.1",
            "17.1.1",
            "17.1.2",
            "17.2",
            "17.2.1",
            "17.3",
            "17.3.1",
            "17.4",
            "17.4.1",
            "17.5",
            "17.5.1"
    );
    private static final int MIDDLEWARE_BUSINESS_PORT = 1120;
    private static final int MIDDLEWARE_PERSONAL_PORT = 1119;

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String model;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private final String modelId;

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

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String address;

    private CompanionDevice(
            String model,
            String modelId,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            List<String> addresses
    ) {
        this(
                model,
                modelId,
                manufacturer,
                platform,
                appVersion,
                osVersion,
                osBuildNumber,
                addresses == null || addresses.isEmpty() ? null : addresses.get(ThreadLocalRandom.current().nextInt(0, addresses.size()))
        );
    }
    
    CompanionDevice(
            String model,
            String modelId,
            String manufacturer,
            PlatformType platform,
            Version appVersion,
            Version osVersion,
            String osBuildNumber,
            String address
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
    }

    public static CompanionDevice web() {
        return web(null);
    }

    public static CompanionDevice web(Version appVersion) {
        return new CompanionDevice(
                "Chrome",
                "Chrome",
                "Google",
                PlatformType.WEB,
                appVersion,
                Version.of("1.0"),
                null,
                List.of()
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
                model.getValue(),
                "Apple",
                business ? PlatformType.IOS_BUSINESS : PlatformType.IOS,
                appVersion,
                Version.of(version),
                "20H320",
                address
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
        return new CompanionDevice(
                "Pixel_2",
                "Pixel_2",
                "Google",
                business ? PlatformType.ANDROID_BUSINESS : PlatformType.ANDROID,
                appVersion,
                Version.of("11"),
                null,
                address
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
                appVersion,
                Version.of("2.5.4"),
                null,
                List.of()
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
        if (platform.isBusiness()) {
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CompanionDevice) obj;
        return Objects.equals(this.model, that.model) &&
                Objects.equals(this.modelId, that.modelId) &&
                Objects.equals(this.manufacturer, that.manufacturer) &&
                Objects.equals(this.platform, that.platform) &&
                Objects.equals(this.appVersion, that.appVersion) &&
                Objects.equals(this.osVersion, that.osVersion) &&
                Objects.equals(this.osBuildNumber, that.osBuildNumber) &&
                Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, modelId, manufacturer, platform, appVersion, osVersion, osBuildNumber, address);
    }

    @Override
    public String toString() {
        return "CompanionDevice[" +
                "model=" + model + ", " +
                "modelId=" + modelId + ", " +
                "manufacturer=" + manufacturer + ", " +
                "platform=" + platform + ", " +
                "appVersion=" + appVersion + ", " +
                "osVersion=" + osVersion + ", " +
                "osBuildNumber=" + osBuildNumber + ", " +
                "address=" + address + ']';
    }
}
