package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The hardware and platform identity a Cobalt client advertises as its
 * underlying device.
 *
 * <p>Cobalt splits the device descriptor into a sealed hierarchy so that the
 * platform-specific fingerprint data is type-safe: {@link Web} (and its native
 * shell refinement {@link Desktop}) carries the synthetic browser fingerprint a
 * companion session blends in with, while {@link Mobile} carries the curated
 * hardware model identifier a primary registration flow advertises. Every
 * flavour exposes the common accessors ({@link #model()},
 * {@link #manufacturer()}, {@link #platform()}, {@link #clientType()},
 * {@link #osDeviceAppVersion()}, {@link #osBuildNumber()}) so transport-agnostic
 * consumers never need to know which concrete flavour they hold.
 *
 * @apiNote
 * Picked at builder time and embedded into the connection handshake so the
 * WhatsApp servers categorise the session for telemetry, feature gating, and
 * User-Agent construction. Cobalt does not auto-detect the host machine; the
 * caller picks one of the pre-built profiles ({@link #web()}, {@link #desktop()},
 * {@link #ios(boolean)}, {@link #android(boolean)}) or constructs one explicitly
 * through the generated builder of the wanted flavour. Mobile factories randomise
 * the model and OS version from a curated list to reduce fingerprintability.
 *
 * @implNote
 * This implementation is the wire-level counterpart of WA Web's
 * {@code DeviceProps} message in {@code WAWebProtobufsCompanionReg.pb}. The
 * {@link #platform()} enum values map one-to-one to
 * {@code DeviceProps$PlatformType}, with the desktop and web flavours pinned per
 * host (Windows hosts pick {@code UWP}, Darwin hosts pick {@code CATALINA}).
 * Because the sealed interface itself cannot be a protobuf {@code MESSAGE} field,
 * it carries a {@link ProtobufSerializer}/{@link ProtobufDeserializer} pair that
 * dispatches on the concrete flavour and delegates to the generated per-flavour
 * spec, prefixing a one-byte discriminator so the reader can pick the matching
 * decoder.
 *
 * @see LinkedWhatsAppClientType
 */
@WhatsAppWebModule(moduleName = "WAWebProtobufsCompanionReg.pb")
// FIXME: Daedalus will include support for sealed @ProtobufMessage, remove the serializer/deserializer when that lands
public sealed interface LinkedWhatsAppClientDevice permits LinkedWhatsAppClientDevice.Web, LinkedWhatsAppClientDevice.Mobile {
    /**
     * Returns a device descriptor configured as a browser-based WhatsApp Web
     * companion.
     *
     * @apiNote
     * Use this when the embedder wants to look like a Chrome WhatsApp Web tab.
     * The wire platform is pinned to {@link ClientPlatformType#WEB} so the server
     * routes through the WebSocket endpoint used by genuine browser sessions.
     *
     * @implNote
     * This implementation hardcodes the Chrome 10.0 identity that WA Web's
     * {@code WAWebClientPayload} emits in {@code ClientPayload.UserAgent} and
     * samples the browser fingerprint from the Windows pool.
     *
     * @return a new web-configured device descriptor
     */
    static LinkedWhatsAppClientDevice web() {
        return Web.random(ClientPlatformType.WEB);
    }

    /**
     * Returns a device descriptor configured as a WhatsApp Desktop companion,
     * auto-detecting the host platform.
     *
     * @apiNote
     * Use this when the embedder wants to look like the native WhatsApp Desktop
     * application. Cobalt's socket layer switches from WebSocket to raw TCP+TLS
     * when the platform is {@link ClientPlatformType#WINDOWS} or
     * {@link ClientPlatformType#MACOS}, matching the transport the Electron-era
     * WhatsApp Desktop app uses.
     *
     * @implNote
     * This implementation reads the JVM's {@code os.name} and picks
     * {@link ClientPlatformType#MACOS} on Darwin hosts and
     * {@link ClientPlatformType#WINDOWS} otherwise; Linux hosts fall back to
     * Windows because there is no native Linux WA Desktop build. The browser
     * fingerprint is sampled from the OS-correlated pool of the chosen platform.
     *
     * @return a desktop-configured device descriptor matching the host platform,
     *         or a Windows descriptor when the host is neither Windows nor macOS
     */
    static LinkedWhatsAppClientDevice desktop() {
        if (Web.IS_MAC) {
            var fingerprint = Web.random(ClientPlatformType.MACOS);
            return new Desktop(
                    "MacBook Pro",
                    "Apple",
                    ClientPlatformType.MACOS,
                    ClientAppVersion.of("14.5"),
                    LinkedWhatsAppClientType.WEB,
                    fingerprint.screenResolution,
                    fingerprint.viewportSize,
                    fingerprint.deviceMemory,
                    fingerprint.hardwareConcurrency,
                    fingerprint.gpuMake,
                    fingerprint.supportedDecoders,
                    fingerprint.connectionRttMs,
                    fingerprint.historyLength
            );
        }
        var fingerprint = Web.random(ClientPlatformType.WINDOWS);
        return new Desktop(
                "Desktop",
                "Microsoft",
                ClientPlatformType.WINDOWS,
                ClientAppVersion.of("10.0"),
                LinkedWhatsAppClientType.WEB,
                fingerprint.screenResolution,
                fingerprint.viewportSize,
                fingerprint.deviceMemory,
                fingerprint.hardwareConcurrency,
                fingerprint.gpuMake,
                fingerprint.supportedDecoders,
                fingerprint.connectionRttMs,
                fingerprint.historyLength
        );
    }

    /**
     * Returns a device descriptor configured as a randomly sampled iOS iPhone.
     *
     * @apiNote
     * Use this when the embedder runs the iOS mobile registration flow and wants
     * a plausible-looking device fingerprint. The model and iOS version are
     * sampled uniformly from the curated pool on every call.
     *
     * @implNote
     * This implementation pins the wire platform to
     * {@link ClientPlatformType#IOS_BUSINESS} when {@code business} is
     * {@code true} and {@link ClientPlatformType#IOS} otherwise; the client type
     * is always {@link LinkedWhatsAppClientType#MOBILE}.
     *
     * @param business {@code true} for the WhatsApp Business variant,
     *                 {@code false} for the consumer variant
     * @return a new iOS-configured device descriptor
     */
    static LinkedWhatsAppClientDevice ios(boolean business) {
        var device = Mobile.IOS_DEVICES.get(DataUtils.randomInt(Mobile.IOS_DEVICES.size()));
        return new Mobile(
                device.model,
                device.manufacturer,
                business ? ClientPlatformType.IOS_BUSINESS : ClientPlatformType.IOS,
                device.osDeviceAppVersion,
                LinkedWhatsAppClientType.MOBILE,
                device.osBuildNumber,
                device.modelId
        );
    }

    /**
     * Returns a device descriptor configured as a randomly generated Google
     * Pixel device.
     *
     * @apiNote
     * Use this when the embedder runs the Android mobile registration flow and
     * wants a plausible-looking device fingerprint. The Pixel model number is
     * sampled in {@code [2, 8]} and the Android version in {@code [11, 15]} on
     * every call.
     *
     * @implNote
     * This implementation pins the wire platform to
     * {@link ClientPlatformType#ANDROID_BUSINESS} when {@code business} is
     * {@code true} and {@link ClientPlatformType#ANDROID} otherwise; the client
     * type is always {@link LinkedWhatsAppClientType#MOBILE}.
     *
     * @param business {@code true} for the WhatsApp Business variant,
     *                 {@code false} for the consumer variant
     * @return a new Android-configured device descriptor
     */
    static LinkedWhatsAppClientDevice android(boolean business) {
        var model = "Pixel_" + DataUtils.randomInt(2, 9);
        return new Mobile(
                model,
                "Google",
                business ? ClientPlatformType.ANDROID_BUSINESS : ClientPlatformType.ANDROID,
                ClientAppVersion.of(String.valueOf(DataUtils.randomInt(11, 16))),
                LinkedWhatsAppClientType.MOBILE,
                null,
                model
        );
    }

    /**
     * Returns the user-facing model name.
     *
     * @return the model name (for example {@code "iPhone 8"} or {@code "Pixel_5"})
     */
    String model();

    /**
     * Returns the manufacturer name.
     *
     * @return the manufacturer (for example {@code "Apple"} or {@code "Google"})
     */
    String manufacturer();

    /**
     * Returns the wire-level platform identifier.
     *
     * @return the platform identifying the operating system and client variant
     */
    ClientPlatformType platform();

    /**
     * Returns the operating system version.
     *
     * @return the OS version
     */
    ClientAppVersion osDeviceAppVersion();

    /**
     * Returns the OS build number.
     *
     * @apiNote
     * A browser {@link Web} descriptor reports {@code null} because a tab has no
     * OS build number; a {@link Desktop} shell reports the host OS version, and a
     * {@link Mobile} descriptor reports the curated build number, falling back to
     * the OS version string when the curated pool carries none.
     *
     * @return the OS build number string, or {@code null} when the platform
     *         reports none
     */
    String osBuildNumber();

    /**
     * Returns the WhatsApp client flavour.
     *
     * @return the client flavour, either {@link LinkedWhatsAppClientType#MOBILE}
     *         or {@link LinkedWhatsAppClientType#WEB}
     */
    LinkedWhatsAppClientType clientType();

    /**
     * Returns a copy of this device with the specified platform.
     *
     * @apiNote
     * General-purpose copy-with for the {@link #platform()} field; used by
     * {@link #toPersonal()} and {@link #toBusiness()}. A {@code null} argument
     * keeps the current platform.
     *
     * @implSpec
     * Implementations return a fresh descriptor of their own concrete flavour
     * with every other component preserved.
     *
     * @param platform the new platform type, or {@code null} to keep the current
     *                 one
     * @return a new device of the same flavour with the given platform
     */
    LinkedWhatsAppClientDevice withPlatform(ClientPlatformType platform);

    /**
     * Returns a copy of this device with the platform switched to the personal
     * (non-business) variant.
     *
     * @apiNote
     * Use this to switch a Business device into a Consumer device mid-session
     * (for example after a downgrade flow). Devices that are already Consumer or
     * that have no business counterpart return {@code this} unchanged.
     *
     * @return this device if already personal, otherwise a new device with the
     *         personal platform variant
     */
    default LinkedWhatsAppClientDevice toPersonal() {
        return switch (platform()) {
            case ANDROID_BUSINESS -> withPlatform(ClientPlatformType.ANDROID);
            case IOS_BUSINESS -> withPlatform(ClientPlatformType.IOS);
            default -> this;
        };
    }

    /**
     * Returns a copy of this device with the platform switched to the business
     * variant.
     *
     * @apiNote
     * Use this to switch a Consumer device into a Business device mid-session
     * (for example after upgrading to WhatsApp Business). Devices that are
     * already Business or that have no business counterpart return {@code this}
     * unchanged.
     *
     * @return this device if already business, otherwise a new device with the
     *         business platform variant
     */
    default LinkedWhatsAppClientDevice toBusiness() {
        return switch (platform()) {
            case ANDROID -> withPlatform(ClientPlatformType.ANDROID_BUSINESS);
            case IOS -> withPlatform(ClientPlatformType.IOS_BUSINESS);
            default -> this;
        };
    }

    /**
     * Returns a User-Agent string suitable for HTTP requests issued by this
     * device.
     *
     * @apiNote
     * Used by HTTP-shaped surfaces (companion-link asset fetches, media uploads,
     * mobile registration). Web and desktop platforms return a stock Chrome
     * User-Agent so the request looks browser-like; mobile platforms return the
     * {@code WhatsApp/<version> <platform>/<os> Device/<model>} shape the native
     * clients emit.
     *
     * @param clientDeviceAppVersion the WhatsApp client version to embed in the
     *                               User-Agent
     * @return the formatted User-Agent string
     * @throws IllegalStateException if the underlying platform enum has an
     *                               unexpected value
     */
    default String toUserAgent(ClientAppVersion clientDeviceAppVersion) {
        var platform = platform();
        if (platform == ClientPlatformType.WINDOWS || platform == ClientPlatformType.MACOS || platform == ClientPlatformType.WEB) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
        }
        var platformName = switch (platform) {
            case ANDROID -> "Android";
            case ANDROID_BUSINESS -> "SMBA";
            case IOS -> "iOS";
            case IOS_BUSINESS -> "SMB iOS";
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
        var deviceName = switch (platform) {
            case ANDROID, ANDROID_BUSINESS -> manufacturer() + "-" + model();
            case IOS, IOS_BUSINESS -> model();
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
        return "WhatsApp/%s %s/%s Device/%s".formatted(
                clientDeviceAppVersion,
                platformName,
                osDeviceAppVersion().toString(),
                deviceName
        );
    }

    /**
     * Serialises this descriptor to a discriminated byte payload.
     *
     * @apiNote
     * This is the {@link ProtobufSerializer} the enclosing store uses to persist
     * the sealed device inside a single protobuf {@code BYTES} field.
     *
     * @implNote
     * This implementation prefixes a one-byte flavour discriminator
     * ({@code 0} = {@link Web}, {@code 1} = {@link Mobile}, {@code 2} =
     * {@link Desktop}) to the payload produced by the generated per-flavour spec,
     * checking {@link Desktop} before {@link Web} because {@code Desktop} is a
     * {@code Web} subtype.
     *
     * @return the discriminated byte payload
     */
    @ProtobufSerializer
    default byte[] serialize() {
        return switch (this) {
            case Desktop desktop -> {
                var messageSize = LinkedWhatsAppClientDeviceDesktopSpec.sizeOf(desktop);
                var body = new byte[messageSize + 1];
                body[0] = Desktop.PROTO_TYPE_TAG;
                LinkedWhatsAppClientDeviceDesktopSpec.encode(desktop, ProtobufOutputStream.toBytes(body, 1));
                yield body;
            }
            case Web web -> {
                var messageSize = LinkedWhatsAppClientDeviceWebSpec.sizeOf(web);
                var body = new byte[messageSize + 1];
                body[0] = Web.PROTO_TYPE_TAG;
                LinkedWhatsAppClientDeviceWebSpec.encode(web, ProtobufOutputStream.toBytes(body, 1));
                yield body;
            }
            case Mobile mobile -> {
                var messageSize = LinkedWhatsAppClientDeviceMobileSpec.sizeOf(mobile);
                var body = new byte[messageSize + 1];
                body[0] = Mobile.PROTO_TYPE_TAG;
                LinkedWhatsAppClientDeviceMobileSpec.encode(mobile, ProtobufOutputStream.toBytes(body, 1));
                yield body;
            }
        };
    }

    /**
     * Reconstructs a descriptor from a discriminated byte payload.
     *
     * @apiNote
     * This is the {@link ProtobufDeserializer} counterpart of {@link #serialize()}.
     *
     * @implNote
     * This implementation reads the leading flavour discriminator and delegates
     * the remaining bytes to the matching generated per-flavour spec.
     *
     * @param serialized the discriminated byte payload, or {@code null}
     * @return the reconstructed descriptor, or {@code null} when the payload is
     *         {@code null} or empty
     * @throws IllegalArgumentException if the discriminator is unrecognised
     */
    @ProtobufDeserializer
    static LinkedWhatsAppClientDevice deserialize(byte[] serialized) {
        if (serialized == null || serialized.length == 0) {
            return null;
        }
        var body = Arrays.copyOfRange(serialized, 1, serialized.length);
        return switch (serialized[0]) {
            case Web.PROTO_TYPE_TAG -> LinkedWhatsAppClientDeviceWebSpec.decode(body);
            case Desktop.PROTO_TYPE_TAG -> LinkedWhatsAppClientDeviceDesktopSpec.decode(body);
            case Mobile.PROTO_TYPE_TAG -> LinkedWhatsAppClientDeviceMobileSpec.decode(body);
            default -> throw new IllegalArgumentException("Unknown device discriminator: " + serialized[0]);
        };
    }

    /**
     * A browser-based WhatsApp Web companion descriptor.
     *
     * <p>Beyond the common device identity this flavour carries the synthetic
     * browser telemetry the session advertises to blend in with a genuine Chrome
     * WhatsApp Web tab. Every fingerprint value is drawn once at pairing by
     * {@link #random(ClientPlatformType)} and persisted with the enclosing device,
     * so the fingerprint stays byte-stable across reconnects and re-logins of the
     * same session. The individual values are correlated so the combination
     * describes one coherent machine: the viewport is derived from the screen
     * resolution, the CPU core count from the reported memory, and the decodable
     * video codecs from the GPU vendor.
     *
     * @apiNote
     * The fingerprint strings mirror the shapes of the corresponding browser APIs
     * ({@code screen.width}/{@code screen.height},
     * {@code navigator.deviceMemory}, {@code navigator.hardwareConcurrency}, the
     * unmasked {@code WEBGL_debug_renderer_info} vendor,
     * {@code navigator.connection.rtt}, {@code window.history.length}) so a
     * server-side fingerprinter sees values consistent with a real tab.
     */
    @ProtobufMessage
    @WhatsAppWebModule(moduleName = "WAWebProtobufsCompanionReg.pb")
    sealed class Web implements LinkedWhatsAppClientDevice permits Desktop {
        /**
         * The protobuf type tag
         */
        private static final byte PROTO_TYPE_TAG = 1;

        /**
         * Whether the current host is a Mac.
         */
        private static final boolean IS_MAC;

        static {
            var osName = System.getProperty("os.name", "").toLowerCase();
            IS_MAC = osName.contains("mac") || osName.contains("darwin");
        }

        /**
         * The user-facing model name (for example {@code "Chrome"}).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String model;

        /**
         * The device manufacturer name (for example {@code "Google Inc."}).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String manufacturer;

        /**
         * The platform identifier the server uses to route the session.
         */
        @WhatsAppWebExport(moduleName = "WAWebProtobufsCompanionReg.pb",
                exports = "DeviceProps$PlatformType", adaptation = WhatsAppAdaptation.DIRECT)
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        final ClientPlatformType platform;

        /**
         * The operating system version running on the device.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        final ClientAppVersion osDeviceAppVersion;

        /**
         * The WhatsApp client flavour the device represents.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
        final LinkedWhatsAppClientType clientType;

        /**
         * The physical screen resolution in {@code WIDTHxHEIGHT} pixels (for
         * example {@code "1920x1080"}).
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        final String screenResolution;

        /**
         * The browser viewport (inner window) size in {@code WIDTHxHEIGHT}
         * pixels, derived from {@link #screenResolution} minus the platform's
         * window chrome and menu bar.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        final String viewportSize;

        /**
         * The device memory in gibibytes as reported by the
         * {@code navigator.deviceMemory} Web API (for example {@code "8"}).
         */
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        final String deviceMemory;

        /**
         * The logical CPU core count as reported by the
         * {@code navigator.hardwareConcurrency} Web API, correlated with
         * {@link #deviceMemory} and capped at {@code 16}.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
        final int hardwareConcurrency;

        /**
         * The unmasked WebGL GPU vendor string (for example
         * {@code "Google Inc. (NVIDIA)"}).
         */
        @ProtobufProperty(index = 10, type = ProtobufType.STRING)
        final String gpuMake;

        /**
         * The comma-separated list of hardware video codecs the browser can
         * decode (for example {@code "avc,hevc,av1,vp9"}), correlated with
         * {@link #gpuMake}.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        final String supportedDecoders;

        /**
         * The estimated network round-trip time in milliseconds as reported by
         * the {@code navigator.connection.rtt} Web API.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.UINT32)
        final int connectionRttMs;

        /**
         * The browsing-history stack length as reported by
         * {@code window.history.length}, in {@code [1, 5]}.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.UINT32)
        final int historyLength;

        /**
         * Constructs a web descriptor from explicit components.
         *
         * @apiNote
         * Package-private; instances reach embedders through the protobuf
         * deserialiser, the generated builder, and the static factories
         * ({@link #web()}, {@link #random(ClientPlatformType)}).
         *
         * @param model               the user-facing model name
         * @param manufacturer        the device manufacturer
         * @param platform            the wire-level platform identifier
         * @param osDeviceAppVersion  the operating system version
         * @param clientType          the WhatsApp client flavour
         * @param screenResolution    the physical screen resolution
         * @param viewportSize        the browser viewport size
         * @param deviceMemory        the reported device memory in gibibytes
         * @param hardwareConcurrency the reported logical CPU core count
         * @param gpuMake             the unmasked WebGL GPU vendor string
         * @param supportedDecoders   the comma-separated decodable codec list
         * @param connectionRttMs     the reported network round-trip time in
         *                            milliseconds
         * @param historyLength       the reported browsing-history stack length
         */
        Web(String model, String manufacturer, ClientPlatformType platform, ClientAppVersion osDeviceAppVersion, LinkedWhatsAppClientType clientType, String screenResolution, String viewportSize, String deviceMemory, int hardwareConcurrency, String gpuMake, String supportedDecoders, int connectionRttMs, int historyLength) {
            this.model = model;
            this.manufacturer = manufacturer;
            this.platform = platform;
            this.osDeviceAppVersion = osDeviceAppVersion;
            this.clientType = clientType;
            this.screenResolution = screenResolution;
            this.viewportSize = viewportSize;
            this.deviceMemory = deviceMemory;
            this.hardwareConcurrency = hardwareConcurrency;
            this.gpuMake = gpuMake;
            this.supportedDecoders = supportedDecoders;
            this.connectionRttMs = connectionRttMs;
            this.historyLength = historyLength;
        }

        /**
         * Returns a coherent, randomly sampled web descriptor for the given
         * platform.
         *
         * <p>The screen resolution and GPU vendor are sampled from OS-specific
         * pools; the remaining fingerprint fields are correlated so the whole
         * descriptor describes one plausible machine. The viewport is the screen
         * minus the window chrome ({@code (W-17)x(H-135)} on Windows,
         * {@code Wx(H-125)} on macOS to account for the menu bar); the CPU core
         * count is derived from the sampled memory; and the decodable codec list
         * is derived from the sampled GPU (discrete and Apple GPUs add AV1,
         * integrated Intel GPUs do not). The browser identity is pinned to Chrome.
         *
         * @implNote
         * This implementation treats {@link ClientPlatformType#MACOS} as the Mac
         * pool and every other value ({@link ClientPlatformType#WEB},
         * {@link ClientPlatformType#WINDOWS}, and the mobile platforms used by the
         * synthetic-telemetry fallback) as the Windows pool. All picks go through
         * {@link DataUtils#randomInt(int)} and {@link DataUtils#randomInt(int, int)},
         * so the sampled machine is fresh per pairing and then frozen by
         * persistence. The device-memory pool {@code {8,8,4,4,4,2}} weights the
         * common values, and the {@code [1, 5]} history length uses
         * {@link DataUtils#randomInt(int, int)} with an exclusive upper bound of
         * {@code 6}.
         *
         * @param platform the platform whose pools to sample from
         * @return a new correlated web descriptor
         */
        public static Web random(ClientPlatformType platform) {
            var mac = platform == ClientPlatformType.MACOS;
            var screens = mac
                    ? new String[]{"2560x1600", "2880x1800", "3024x1964", "1470x956", "1512x982"}
                    : new String[]{"1920x1080", "1366x768", "1536x864", "2560x1440", "1440x900", "1600x900", "3840x2160"};
            var screenResolution = screens[DataUtils.randomInt(screens.length)];
            var separator = screenResolution.indexOf('x');
            var screenWidth = Integer.parseInt(screenResolution.substring(0, separator));
            var screenHeight = Integer.parseInt(screenResolution.substring(separator + 1));
            var viewportWidth = mac ? screenWidth : screenWidth - 17;
            var viewportHeight = mac ? screenHeight - 125 : screenHeight - 135;
            var viewportSize = viewportWidth + "x" + viewportHeight;
            var historyLength = DataUtils.randomInt(1, 6);
            var memoryPool = new String[]{"8", "8", "4", "4", "4", "2"};
            var deviceMemory = memoryPool[DataUtils.randomInt(memoryPool.length)];
            var hardwareConcurrency = deviceMemory.equals("2")
                    ? (DataUtils.randomInt(2) == 0 ? 2 : 4)
                    : (DataUtils.randomInt(2) == 0 ? 4 : 8);
            hardwareConcurrency = Math.min(hardwareConcurrency, 16);
            var gpus = mac
                    ? new String[]{"Google Inc. (Apple)", "Apple GPU", "Google Inc. (Intel)"}
                    : new String[]{"Google Inc. (Intel)", "Google Inc. (NVIDIA)", "Google Inc. (AMD)"};
            var gpuMake = gpus[DataUtils.randomInt(gpus.length)];
            var supportedDecoders = gpuMake.contains("Intel")
                    ? (DataUtils.randomInt(2) == 0 ? "avc,hevc,vp9" : "avc,vp9")
                    : "avc,hevc,av1,vp9";
            var rttPool = new int[]{0, 50, 50, 100};
            var connectionRttMs = rttPool[DataUtils.randomInt(rttPool.length)];
            return new Web(
                    "Chrome",
                    "Google Inc.",
                    platform,
                    ClientAppVersion.of("10.0"),
                    LinkedWhatsAppClientType.WEB,
                    screenResolution,
                    viewportSize,
                    deviceMemory,
                    hardwareConcurrency,
                    gpuMake,
                    supportedDecoders,
                    connectionRttMs,
                    historyLength
            );
        }

        @Override
        public String model() {
            return model;
        }

        @Override
        public String manufacturer() {
            return manufacturer;
        }

        @Override
        public ClientPlatformType platform() {
            return platform;
        }

        @Override
        public ClientAppVersion osDeviceAppVersion() {
            return osDeviceAppVersion;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * A browser tab has no operating-system build number, so this
         * implementation returns {@code null}.
         */
        @Override
        public String osBuildNumber() {
            return null;
        }

        @Override
        public LinkedWhatsAppClientType clientType() {
            return clientType;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation preserves the browser fingerprint and returns a
         * new {@link Web} carrying the requested platform.
         */
        @Override
        public LinkedWhatsAppClientDevice withPlatform(ClientPlatformType platform) {
            return new Web(
                    model,
                    manufacturer,
                    Objects.requireNonNullElse(platform, this.platform),
                    osDeviceAppVersion,
                    clientType,
                    screenResolution,
                    viewportSize,
                    deviceMemory,
                    hardwareConcurrency,
                    gpuMake,
                    supportedDecoders,
                    connectionRttMs,
                    historyLength
            );
        }

        /**
         * Returns the physical screen resolution.
         *
         * @return the screen resolution in {@code WIDTHxHEIGHT} pixels
         */
        public String screenResolution() {
            return screenResolution;
        }

        /**
         * Returns the browser viewport size.
         *
         * @return the viewport size in {@code WIDTHxHEIGHT} pixels
         */
        public String viewportSize() {
            return viewportSize;
        }

        /**
         * Returns the reported device memory.
         *
         * @return the device memory in gibibytes
         */
        public String deviceMemory() {
            return deviceMemory;
        }

        /**
         * Returns the reported logical CPU core count.
         *
         * @return the hardware concurrency
         */
        public int hardwareConcurrency() {
            return hardwareConcurrency;
        }

        /**
         * Returns the unmasked WebGL GPU vendor string.
         *
         * @return the GPU vendor
         */
        public String gpuMake() {
            return gpuMake;
        }

        /**
         * Returns the comma-separated list of decodable video codecs.
         *
         * @return the supported decoders
         */
        public String supportedDecoders() {
            return supportedDecoders;
        }

        /**
         * Returns the reported network round-trip time.
         *
         * @return the connection round-trip time in milliseconds
         */
        public int connectionRttMs() {
            return connectionRttMs;
        }

        /**
         * Returns the reported browsing-history stack length.
         *
         * @return the history length in {@code [1, 5]}
         */
        public int historyLength() {
            return historyLength;
        }

        /**
         * Compares this descriptor to another object for structural equality.
         *
         * @param o the object to compare with
         * @return {@code true} if the descriptors are structurally equal
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Web that
                    && hardwareConcurrency == that.hardwareConcurrency
                    && connectionRttMs == that.connectionRttMs
                    && historyLength == that.historyLength
                    && Objects.equals(model, that.model)
                    && Objects.equals(manufacturer, that.manufacturer)
                    && platform == that.platform
                    && Objects.equals(osDeviceAppVersion, that.osDeviceAppVersion)
                    && clientType == that.clientType
                    && Objects.equals(screenResolution, that.screenResolution)
                    && Objects.equals(viewportSize, that.viewportSize)
                    && Objects.equals(deviceMemory, that.deviceMemory)
                    && Objects.equals(gpuMake, that.gpuMake)
                    && Objects.equals(supportedDecoders, that.supportedDecoders);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(model, manufacturer, platform, osDeviceAppVersion, clientType,
                    screenResolution, viewportSize, deviceMemory, hardwareConcurrency, gpuMake,
                    supportedDecoders, connectionRttMs, historyLength);
        }

        /**
         * Returns a human-readable description suitable for logs.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "LinkedWhatsAppClientDevice.Web[" +
                    "model='" + model + '\'' +
                    ", manufacturer='" + manufacturer + '\'' +
                    ", platform=" + platform +
                    ", osDeviceAppVersion=" + osDeviceAppVersion +
                    ", clientType=" + clientType +
                    ", screenResolution='" + screenResolution + '\'' +
                    ']';
        }
    }

    /**
     * A native WhatsApp Desktop shell descriptor.
     *
     * <p>The desktop application is a native shell around the same WhatsApp Web
     * surface, so it is modelled as a {@link Web} refinement: it carries the same
     * browser fingerprint but advertises a native OS build number rather than the
     * {@code null} a browser tab reports.
     */
    @ProtobufMessage
    @WhatsAppWebModule(moduleName = "WAWebProtobufsCompanionReg.pb")
    final class Desktop extends Web {
        /**
         * The protobuf type tag
         */
        private static final byte PROTO_TYPE_TAG = 2;

        /**
         * Constructs a desktop descriptor from explicit components.
         *
         * @apiNote
         * Package-private; instances reach embedders through the protobuf
         * deserialiser, the generated builder, and {@link #desktop()}.
         *
         * @param model               the user-facing model name
         * @param manufacturer        the device manufacturer
         * @param platform            the wire-level platform identifier
         * @param osDeviceAppVersion  the operating system version
         * @param clientType          the WhatsApp client flavour
         * @param screenResolution    the physical screen resolution
         * @param viewportSize        the browser viewport size
         * @param deviceMemory        the reported device memory in gibibytes
         * @param hardwareConcurrency the reported logical CPU core count
         * @param gpuMake             the unmasked WebGL GPU vendor string
         * @param supportedDecoders   the comma-separated decodable codec list
         * @param connectionRttMs     the reported network round-trip time in
         *                            milliseconds
         * @param historyLength       the reported browsing-history stack length
         */
        Desktop(String model, String manufacturer, ClientPlatformType platform, ClientAppVersion osDeviceAppVersion, LinkedWhatsAppClientType clientType, String screenResolution, String viewportSize, String deviceMemory, int hardwareConcurrency, String gpuMake, String supportedDecoders, int connectionRttMs, int historyLength) {
            super(model, manufacturer, platform, osDeviceAppVersion, clientType, screenResolution, viewportSize, deviceMemory, hardwareConcurrency, gpuMake, supportedDecoders, connectionRttMs, historyLength);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * The native desktop shell reports the host OS version as its build
         * number, so this implementation returns the OS version string.
         */
        @Override
        public String osBuildNumber() {
            return osDeviceAppVersion.toString();
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation preserves the browser fingerprint and returns a
         * new {@link Desktop} carrying the requested platform.
         */
        @Override
        public LinkedWhatsAppClientDevice withPlatform(ClientPlatformType platform) {
            return new Desktop(
                    model,
                    manufacturer,
                    Objects.requireNonNullElse(platform, this.platform),
                    osDeviceAppVersion,
                    clientType,
                    screenResolution,
                    viewportSize,
                    deviceMemory,
                    hardwareConcurrency,
                    gpuMake,
                    supportedDecoders,
                    connectionRttMs,
                    historyLength
            );
        }

        /**
         * Returns a human-readable description suitable for logs.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "LinkedWhatsAppClientDevice.Desktop[" +
                    "model='" + model + '\'' +
                    ", manufacturer='" + manufacturer + '\'' +
                    ", platform=" + platform +
                    ", osDeviceAppVersion=" + osDeviceAppVersion +
                    ", clientType=" + clientType +
                    ", screenResolution='" + screenResolution + '\'' +
                    ']';
        }
    }

    /**
     * A primary WhatsApp Mobile device descriptor.
     *
     * <p>This flavour models a native iOS or Android handset registered as a
     * primary device. It carries the internal hardware model identifier the
     * native clients advertise and the OS build number of the sampled handset,
     * and holds the curated pool of plausible iOS handsets the {@link #ios(boolean)}
     * factory samples from.
     */
    @ProtobufMessage
    @WhatsAppWebModule(moduleName = "WAWebProtobufsCompanionReg.pb")
    final class Mobile implements LinkedWhatsAppClientDevice {
        /**
         * The protobuf type tag
         */
        private static final byte PROTO_TYPE_TAG = 3;

        /**
         * The pool of iOS device fingerprints the {@link #ios(boolean)} factory
         * samples from.
         *
         * @apiNote
         * Each entry pairs a real iPhone model with a real iOS version, build
         * number, and internal model identifier so the resulting
         * {@code DeviceProps} payload looks plausible to server-side heuristics.
         *
         * @implNote
         * This implementation samples uniformly via {@link DataUtils#randomInt(int)}
         * on every call to {@link #ios(boolean)}; there is no caching so each
         * registered session is independent. The platform of each pool entry is
         * {@code null} and is pinned by {@link #ios(boolean)} to the consumer or
         * business variant on sampling.
         */
        static final List<Mobile> IOS_DEVICES = List.of(
                new Mobile("iPhone 7", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone9,3"),
                new Mobile("iPhone 7", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone9,3"),
                new Mobile("iPhone 7 Plus", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone9,4"),
                new Mobile("iPhone 7 Plus", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone9,4"),
                new Mobile("iPhone 8", "Apple", null, ClientAppVersion.of("13.7"), LinkedWhatsAppClientType.MOBILE, "17H35", "iPhone10,4"),
                new Mobile("iPhone 8", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone10,4"),
                new Mobile("iPhone 8", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone10,4"),
                new Mobile("iPhone 8", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone10,4"),
                new Mobile("iPhone 8 Plus", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone10,5"),
                new Mobile("iPhone 8 Plus", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone10,5"),
                new Mobile("iPhone 8 Plus", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone10,5"),
                new Mobile("iPhone X", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone10,6"),
                new Mobile("iPhone X", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone10,6"),
                new Mobile("iPhone X", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone10,6"),
                new Mobile("iPhone XR", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone11,8"),
                new Mobile("iPhone XR", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone11,8"),
                new Mobile("iPhone XR", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone11,8"),
                new Mobile("iPhone XR", "Apple", null, ClientAppVersion.of("17.4.1"), LinkedWhatsAppClientType.MOBILE, "21E236", "iPhone11,8"),
                new Mobile("iPhone XS", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone11,2"),
                new Mobile("iPhone XS", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone11,2"),
                new Mobile("iPhone XS", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone11,2"),
                new Mobile("iPhone XS", "Apple", null, ClientAppVersion.of("17.4.1"), LinkedWhatsAppClientType.MOBILE, "21E236", "iPhone11,2"),
                new Mobile("iPhone XS Max", "Apple", null, ClientAppVersion.of("14.8.1"), LinkedWhatsAppClientType.MOBILE, "18H107", "iPhone11,6"),
                new Mobile("iPhone XS Max", "Apple", null, ClientAppVersion.of("15.8.2"), LinkedWhatsAppClientType.MOBILE, "19H384", "iPhone11,6"),
                new Mobile("iPhone XS Max", "Apple", null, ClientAppVersion.of("16.7.7"), LinkedWhatsAppClientType.MOBILE, "20H330", "iPhone11,6"),
                new Mobile("iPhone XS Max", "Apple", null, ClientAppVersion.of("17.4.1"), LinkedWhatsAppClientType.MOBILE, "21E236", "iPhone11,6")
        );

        /**
         * The user-facing model name (for example {@code "iPhone 8"} or
         * {@code "Pixel_5"}).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String model;

        /**
         * The device manufacturer name (for example {@code "Apple"} or
         * {@code "Google"}).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String manufacturer;

        /**
         * The platform identifier the server uses to route the session.
         */
        @WhatsAppWebExport(moduleName = "WAWebProtobufsCompanionReg.pb",
                exports = "DeviceProps$PlatformType", adaptation = WhatsAppAdaptation.DIRECT)
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        final ClientPlatformType platform;

        /**
         * The operating system version running on the device (for example
         * {@code 16.7.7} for iOS or {@code 14} for Android).
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        final ClientAppVersion osDeviceAppVersion;

        /**
         * The WhatsApp client flavour the device represents.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
        final LinkedWhatsAppClientType clientType;

        /**
         * The OS build number (for example {@code "20H330"} for iOS), or
         * {@code null} on platforms where the build number is not applicable.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        final String osBuildNumber;

        /**
         * The internal hardware model identifier (for example
         * {@code "iPhone10,4"} for iPhone 8 or {@code "Pixel_5"} for Google
         * Pixel 5).
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        final String modelId;

        /**
         * Constructs a mobile descriptor from explicit components.
         *
         * @apiNote
         * Package-private; instances reach embedders through the protobuf
         * deserialiser, the generated builder, and the static factories
         * ({@link #ios(boolean)}, {@link #android(boolean)}).
         *
         * @param model              the user-facing model name
         * @param manufacturer       the device manufacturer
         * @param platform           the wire-level platform identifier, or
         *                           {@code null} for a curated-pool entry
         * @param osDeviceAppVersion the operating system version
         * @param clientType         the WhatsApp client flavour
         * @param osBuildNumber      the OS build number, or {@code null}
         * @param modelId            the internal hardware model identifier
         */
        Mobile(String model, String manufacturer, ClientPlatformType platform, ClientAppVersion osDeviceAppVersion, LinkedWhatsAppClientType clientType, String osBuildNumber, String modelId) {
            this.model = model;
            this.manufacturer = manufacturer;
            this.platform = platform;
            this.osDeviceAppVersion = osDeviceAppVersion;
            this.clientType = clientType;
            this.osBuildNumber = osBuildNumber;
            this.modelId = modelId;
        }

        @Override
        public String model() {
            return model;
        }

        @Override
        public String manufacturer() {
            return manufacturer;
        }

        @Override
        public ClientPlatformType platform() {
            return platform;
        }

        @Override
        public ClientAppVersion osDeviceAppVersion() {
            return osDeviceAppVersion;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns the curated build number, falling back to
         * the OS version string when the sampled handset carries none (for
         * example an Android descriptor).
         */
        @Override
        public String osBuildNumber() {
            return Objects.requireNonNullElse(osBuildNumber, osDeviceAppVersion.toString());
        }

        @Override
        public LinkedWhatsAppClientType clientType() {
            return clientType;
        }

        /**
         * Returns the internal hardware model identifier.
         *
         * @return the model identifier (for example {@code "iPhone10,4"})
         */
        public String modelId() {
            return modelId;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation preserves the hardware identity and returns a new
         * {@link Mobile} carrying the requested platform.
         */
        @Override
        public LinkedWhatsAppClientDevice withPlatform(ClientPlatformType platform) {
            return new Mobile(
                    model,
                    manufacturer,
                    Objects.requireNonNullElse(platform, this.platform),
                    osDeviceAppVersion,
                    clientType,
                    osBuildNumber,
                    modelId
            );
        }

        /**
         * Compares this descriptor to another object for structural equality.
         *
         * @param o the object to compare with
         * @return {@code true} if the descriptors are structurally equal
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Mobile that
                    && Objects.equals(model, that.model)
                    && Objects.equals(manufacturer, that.manufacturer)
                    && platform == that.platform
                    && Objects.equals(osDeviceAppVersion, that.osDeviceAppVersion)
                    && clientType == that.clientType
                    && Objects.equals(osBuildNumber, that.osBuildNumber)
                    && Objects.equals(modelId, that.modelId);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(model, manufacturer, platform, osDeviceAppVersion, clientType, osBuildNumber, modelId);
        }

        /**
         * Returns a human-readable description suitable for logs.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "LinkedWhatsAppClientDevice.Mobile[" +
                    "model='" + model + '\'' +
                    ", manufacturer='" + manufacturer + '\'' +
                    ", platform=" + platform +
                    ", osDeviceAppVersion=" + osDeviceAppVersion +
                    ", osBuildNumber='" + osBuildNumber + '\'' +
                    ", modelId='" + modelId + '\'' +
                    ", clientType=" + clientType +
                    ']';
        }
    }
}
