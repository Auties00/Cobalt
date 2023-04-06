package it.auties.whatsapp.api;

import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.controller.ControllerDeserializer;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.signal.auth.Version;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * A model class that contains the registration options for a {@link Whatsapp} instance
 */
@SuperBuilder
@Data
@Accessors(fluent = true)
public abstract sealed class WhatsappOptions permits WebOptions, MobileOptions {
    /**
     * Constant for unlimited listeners size
     */
    private static final int UNLIMITED_LISTENERS = -1;
    /**
     * The default executor, mirrors {@link java.util.concurrent.CompletableFuture} implementation
     */
    private static final Executor DEFAULT_EXECUTOR = ForkJoinPool.getCommonPoolParallelism() > 1 ? ForkJoinPool.commonPool() : runnable -> new Thread(runnable).start();
    /**
     * The id of the session
     */
    @Default
    private UUID uuid = UUID.randomUUID();
    /**
     * Whether listeners marked with @RegisteredListener should be registered automatically. By
     * default, this option is enabled.
     */
    @Default
    private boolean autodetectListeners = true;
    /**
     * Whether the default serialization mechanism should be used or not. Set this to false if you
     * want to implement a custom serializer.
     */
    @Default
    private ControllerSerializer serializer = new DefaultControllerSerializer();
    /**
     * Whether the default serialization mechanism should be used or not. Set this to false if you
     * want to implement a custom serializer.
     */
    @Default
    private ControllerDeserializer deserializer = new DefaultControllerSerializer();
    /**
     * Whether a preview should be automatically generated and attached to text messages that
     * contain links. By default, it's enabled with inference.
     */
    @Default
    private TextPreviewSetting textPreviewSetting = TextPreviewSetting.ENABLED_WITH_INFERENCE;
    /**
     * The executor to use for the WebSocket
     * Introduced because of <a href="https://github.com/Auties00/Whatsapp4j/issues/223">223</a>
     */
    @Default
    private Executor socketService = DEFAULT_EXECUTOR;
    /**
     * Handles failures in the WebSocket. By default, uses the simple handler and prints to the
     * terminal.
     */
    @Default
    private ErrorHandler errorHandler = ErrorHandler.toTerminal();
    /**
     * The number of maximum listeners that the linked Whatsapp instance supports. By default,
     * unlimited.
     */
    @Default
    private int listenersLimit = UNLIMITED_LISTENERS;

    /**
     * Returns the type of client
     *
     * @return the non-null client type
     */
    @NonNull
    public abstract ClientType clientType();

    /**
     * The version of WhatsappWeb to use. If the version is too outdated, the server will refuse to connect.
     */
    @NonNull
    public abstract Version version();

    /**
     * The name of the os running the client, can be fake
     */
    public abstract String osName();

    /**
     * The version of the os running the client, can be fake
     */
    public abstract String osVersion();

    /**
     * The name of the device running the client, can be fake
     */
    public abstract String deviceName();

    /**
     * The manufacturer of the device running the client, can be fake
     */
    public abstract String deviceManufacturer();

    /**
     * Options for the web client
     */
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    @Data
    @Accessors(fluent = true)
    public final static class WebOptions extends WhatsappOptions {
        /**
         * The version of WhatsappWeb to use. If the version is too outdated, the server will refuse to
         * connect.
         */
        @Default
        private final Version version = Version.latest(ClientType.WEB_CLIENT);
        /**
         * The description provided to Whatsapp during the authentication process. This should be, for
         * example, the name of your service. By default, it's WhatsappWeb4j.
         */
        @Default
        @NonNull
        private String name = "WhatsappWeb4j";

        /**
         * Describes how much chat history Whatsapp should send when the QR is first scanned. By default,
         * one year.
         */
        @Default
        @NonNull
        private HistoryLength historyLength = HistoryLength.ONE_YEAR;

        /**
         * Handles the qr countryCode when a connection is first established with Whatsapp. By default, the qr
         * countryCode is printed on the terminal.
         */
        @Default
        @NonNull
        private QrHandler qrHandler = QrHandler.toTerminal();

        /**
         * Constructs a new instance with default options
         *
         * @return a non-null instance
         */
        public static WebOptions defaultOptions() {
            return WebOptions.builder().build();
        }

        /**
         * Returns the type of client
         *
         * @return the non-null client type
         */
        @NonNull
        public ClientType clientType() {
            return ClientType.WEB_CLIENT;
        }

        @Override
        public String osName() {
            return "Windows";
        }

        @Override
        public String osVersion() {
            return "11";
        }

        @Override
        public String deviceName() {
            return "Laptop";
        }

        @Override
        public String deviceManufacturer() {
            return "Microsoft";
        }
    }

    /**
     * Options for the app client
     */
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    @Data
    @Accessors(fluent = true)
    public final static class MobileOptions extends WhatsappOptions {
        /**
         * The version of WhatsappWeb to use. If the version is too outdated, the server will refuse to
         * connect.
         */
        @Default
        private final Version version = Version.latest(ClientType.APP_CLIENT);
        /**
         * The phone number to register, including the prefix
         */
        @NonNull
        private String phoneNumber;

        /**
         * The pushname of the registered user
         * This is usually set just after registering in the mobile app
         */
        @Default
        @NonNull
        private String name = "Whatsapp4j";

        /**
         * The method to use to confirm the phone number
         */
        @Default
        @NonNull
        private VerificationCodeMethod verificationCodeMethod = VerificationCodeMethod.SMS;

        /**
         * A function to retrieve the OTP sent to the registered phone number
         * The first parameter can be null if the {@link MobileOptions#verificationCodeMethod} was set to {@link VerificationCodeMethod#NONE}
         */
        @NonNull
        private Function<VerificationCodeResponse, String> verificationCodeHandler;

        /**
         * Returns the type of client
         *
         * @return the non-null client type
         */
        @NonNull
        public ClientType clientType() {
            return ClientType.APP_CLIENT;
        }

        @Override
        public String osName() {
            return "iOS";
        }

        @Override
        public String osVersion() {
            return "15.3.1";
        }

        @Override
        public String deviceName() {
            return "iPhone_7";
        }

        @Override
        public String deviceManufacturer() {
            return "Apple";
        }
    }
}
