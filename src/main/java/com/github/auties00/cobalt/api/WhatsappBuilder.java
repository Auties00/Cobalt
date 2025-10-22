package com.github.auties00.cobalt.api;

import com.github.auties00.cobalt.client.mobile.WhatsappMobileClientRegistration;
import com.github.auties00.cobalt.model.business.BusinessCategory;
import com.github.auties00.cobalt.model.jid.JidDevice;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.store.WhatsappStoreSerializer;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A builder for WhatsApp API client instances with support for both web-based and mobile-based connections.
 * <p>
 * This class implements a fluent builder pattern with specialized inner classes that handle:
 * <ul>
 *   <li>Web client connections (through QR codes or pairing codes)</li>
 *   <li>Mobile client connections (through phone numbers and verification)</li>
 *   <li>Custom client configurations for advanced use cases</li>
 * </ul>
 * <p>
 * The builder provides a clean, type-safe API for creating and configuring WhatsApp client instances
 * with appropriate connection, serialization, and authentication options.
 */

public sealed class WhatsappBuilder {
    static final WhatsappBuilder INSTANCE = new WhatsappBuilder();

    private WhatsappBuilder() {

    }

    /**
     * Creates a web client with the default Protobuf serializer
     *
     * @return a non-null web client instance
     */
    public Client.Web webClient() {
        return new Client.Web(WhatsappStoreSerializer.toProtobuf());
    }

    /**
     * Creates a web client with a custom serializer
     *
     * @param serializer the serializer to use for data persistence, must not be null
     * @return a non-null web client instance
     * @throws NullPointerException if serializer is null
     */
    public Client.Web webClient(WhatsappStoreSerializer serializer) {
        Objects.requireNonNull(serializer, "serializer must not be null");
        return new Client.Web(serializer);
    }

    /**
     * Creates a mobile client with the default Protobuf serializer
     *
     * @return a non-null mobile client instance
     */
    public Client.Mobile mobileClient() {
        return new Client.Mobile(WhatsappStoreSerializer.toProtobuf());
    }

    /**
     * Creates a mobile client with a custom serializer
     *
     * @param serializer the serializer to use for data persistence, must not be null
     * @return a non-null mobile client instance
     * @throws NullPointerException if serializer is null
     */
    public Client.Mobile mobileClient(WhatsappStoreSerializer serializer) {
        Objects.requireNonNull(serializer, "serializer must not be null");
        return new Client.Mobile(serializer);
    }

    /**
     * Creates a custom client for advanced configuration
     *
     * @return a non-null custom client instance
     */
    public Custom customClient() {
        return new Custom();
    }

    public static abstract sealed class Client extends WhatsappBuilder {
        final WhatsappStoreSerializer serializer;

        private Client(WhatsappStoreSerializer serializer) {
            this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        }

        /**
         * Creates a new connection using a random UUID
         *
         * @return a non-null options selector
         */
        public abstract Options createConnection();

        /**
         * Loads a connection from the six parts key representation
         *
         * @param sixParts the six parts keys to use to create the connection, must not be null
         * @return a non-null options selector
         * @throws NullPointerException if sixParts is null
         */
        public abstract Options loadConnection(WhatsappSixPartsKeys sixParts);

        /**
         * Loads the last serialized connection.
         * If no connection is available, an empty {@link Optional} will be returned.
         *
         * @return an {@link Optional} containing the last serialized connection, empty otherwise
         */
        public abstract Optional<Options> loadLastConnection();

        /**
         * Loads the last serialized connection.
         * If no connection is available, a new one will be created.
         *
         * @return a non-null options selector
         */
        public abstract Options loadLastOrCreateConnection();

        /**
         * Loads the connection whose id matches {@code uuid}.
         * If {@code uuid} is null, or if no connection has an id that matches {@code uuid}, an empty {@link Optional} will be returned.
         *
         * @param uuid the id to use for the connection; can be null
         * @return an {@link Optional} containing the connection whose id matches {@code uuid}, empty otherwise
         */
        public abstract Optional<Options> loadConnection(UUID uuid);

        /**
         * Loads the connection whose id matches {@code uuid}.
         * If {@code uuid} is null, or if no connection has an id that matches {@code uuid}, a new connection will be created.
         *
         * @param uuid the id to use for the connection; can be null
         * @return a non-null options selector
         */
        public abstract Options loadOrCreateConnection(UUID uuid);

        /**
         * Loads the connection whose phone number matches the given UUID.
         * If the UUID is null, or if no connection matches the given UUID, a new connection will be created.
         *
         * @param phoneNumber the phone value to use to create the connection, can be null (will generate a random UUID)
         * @return a non-null options selector
         */
        public abstract Optional<Options> loadConnection(Long phoneNumber);

        /**
         * Loads the connection whose id matches {@code phoneNumber}.
         * If {@code phoneNumber} is null, or if no connection matches {@code phoneNumber}, a new connection will be created.
         *
         * @param phoneNumber the id to use for the connection, can be null
         * @return a non-null options selector
         */
        public abstract Options loadOrCreateConnection(Long phoneNumber);

        private static WhatsappStore newStore(UUID id, Long phoneNumber, WhatsappClientType clientType, SignalIdentityKeyPair identityKeyPair, SignalIdentityKeyPair noiseKeyPair, boolean registered, byte[] identityId) {
            var device = switch (clientType) {
                case WEB -> JidDevice.web();
                case MOBILE -> JidDevice.ios(false);
            };
            return new WhatsappStoreBuilder()
                    .uuid(Objects.requireNonNullElseGet(id, UUID::randomUUID))
                    .phoneNumber(phoneNumber)
                    .clientType(Objects.requireNonNull(clientType, "clientType must not be null"))
                    .device(device)
                    .identityId(identityId)
                    .identityKeyPair(identityKeyPair)
                    .noiseKeyPair(noiseKeyPair)
                    .registered(registered)
                    .build();
        }

        public static final class Web extends Client {
            private Web(WhatsappStoreSerializer serializer) {
                super(serializer);
            }

            /**
             * Creates a new connection using a random UUID
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Web createConnection() {
                return loadOrCreateConnection(UUID.randomUUID());
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Web loadLastOrCreateConnection() {
                var uuids = serializer.listIds(WhatsappClientType.WEB);
                if(uuids.isEmpty()) {
                    return createConnection();
                }else {
                    return loadOrCreateConnection(uuids.getLast());
                }
            }

            @Override
            public Optional<Options> loadConnection(UUID uuid) {
                if (uuid == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsappClientType.WEB, uuid, null)
                        .map(Options.Web::new);
            }

            /**
             * Creates a new connection using a unique identifier
             * If a session with the given id already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param uuid the UUID to use to create the connection, can be null (will generate a random UUID)
             * @return a non-null options selector
             */
            @Override
            public Options.Web loadOrCreateConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var store = serializer.startDeserialize(WhatsappClientType.WEB, sessionUuid, null)
                        .orElseGet(() -> newStore(sessionUuid, null, WhatsappClientType.WEB, null, null, false, null));
                return new Options.Web(store);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) {
                if (phoneNumber == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsappClientType.WEB, null, phoneNumber)
                        .map(Options.Web::new);
            }

            /**
             * Creates a new connection using a phone value
             * If a session with the given phone value already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param phoneNumber the phone value to use to create the connection, can be null (will generate a random UUID)
             * @return a non-null options selector
             */
            @Override
            public Options.Web loadOrCreateConnection(Long phoneNumber) {
                var store = serializer.startDeserialize(WhatsappClientType.WEB, null, phoneNumber)
                        .orElseGet(() -> newStore(null, phoneNumber, WhatsappClientType.WEB, null, null, false, null));
                return new Options.Web(store);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the six parts keys to use to create the connection, must not be null
             * @return a non-null options selector
             * @throws NullPointerException if sixParts is null
             */
            @Override
            public Options.Web loadConnection(WhatsappSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.startDeserialize(WhatsappClientType.WEB, null, sixParts.phoneNumber());
                if(serialized.isPresent()) {
                    return new Options.Web(serialized.get());
                }

                var store = newStore(null, sixParts.phoneNumber(), WhatsappClientType.WEB, sixParts.identityKeyPair(), sixParts.noiseKeyPair(), true, sixParts.identityId());
                return new Options.Web(store);
            }

            @Override
            public Optional<Options> loadLastConnection() {
                var uuids = serializer.listIds(WhatsappClientType.WEB);
                if(uuids.isEmpty()) {
                    return Optional.empty();
                }else {
                    return loadConnection(uuids.getLast());
                }
            }
        }

        public static final class Mobile extends Client {
            private Mobile(WhatsappStoreSerializer serializer) {
                super(serializer);
            }

            /**
             * Creates a new connection using a random UUID
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile createConnection() {
                return loadOrCreateConnection(UUID.randomUUID());
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile loadLastOrCreateConnection() {
                var uuids = serializer.listIds(WhatsappClientType.MOBILE);
                if(uuids.isEmpty()) {
                    return createConnection();
                }else {
                    return loadOrCreateConnection(uuids.getLast());
                }
            }

            @Override
            public Optional<Options> loadConnection(UUID uuid) {
                if(uuid == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsappClientType.MOBILE, uuid, null)
                        .map(Options.Mobile::new);
            }

            /**
             * Creates a new connection using a unique identifier
             * If a session with the given id already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param uuid the UUID to use to create the connection, can be null (will generate a random UUID)
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile loadOrCreateConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var store = serializer.startDeserialize(WhatsappClientType.MOBILE, sessionUuid, null)
                        .orElseGet(() -> newStore(sessionUuid, null, WhatsappClientType.MOBILE, null, null, false, null));
                return new Options.Mobile(store);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) {
                if(phoneNumber == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsappClientType.MOBILE, null, phoneNumber)
                        .map(Options.Mobile::new);
            }

            /**
             * Creates a new connection using a phone value
             * If a session with the given phone value already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param phoneNumber the phone value to use to create the connection, can be null (will generate a random UUID)
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile loadOrCreateConnection(Long phoneNumber) {
                var store = serializer.startDeserialize(WhatsappClientType.MOBILE, null, phoneNumber)
                        .orElseGet(() -> newStore(null, phoneNumber, WhatsappClientType.MOBILE, null, null, false, null));
                return new Options.Mobile(store);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the six parts keys to use to create the connection, must not be null
             * @return a non-null options selector
             * @throws NullPointerException if sixParts is null
             */
            @Override
            public Options.Mobile loadConnection(WhatsappSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.startDeserialize(WhatsappClientType.MOBILE, null, sixParts.phoneNumber());
                if(serialized.isPresent()) {
                    return new Options.Mobile(serialized.get());
                }

                var store = newStore(null, sixParts.phoneNumber(), WhatsappClientType.MOBILE, sixParts.identityKeyPair(), sixParts.noiseKeyPair(), true, sixParts.identityId());
                return new Options.Mobile(store);
            }

            @Override
            public Optional<Options> loadLastConnection() {
                var uuids = serializer.listIds(WhatsappClientType.MOBILE);
                if(uuids.isEmpty()) {
                    return Optional.empty();
                }else {
                    return loadConnection(uuids.getLast());
                }
            }
        }
    }

    public static sealed class Options extends WhatsappBuilder {
        final WhatsappStore store;
        WhatsappMessagePreviewHandler messagePreviewHandler;
        WhatsappErrorHandler errorHandler;

        private Options(WhatsappStore store) {
            this.store = Objects.requireNonNull(store, "store must not be null");
        }

        /**
         * Sets a proxy for the connection
         *
         * @param proxy the proxy to use, can be null to use no proxy
         * @return the same instance for chaining
         */
        public Options proxy(URI proxy) {
            store.setProxy(proxy);
            return this;
        }

        /**
         * Sets the companion device for the connection
         *
         * @param device the companion device, can be null
         * @return the same instance for chaining
         */
        public Options device(JidDevice device) {
            store.setDevice(device);
            return this;
        }

        /**
         * Controls whether the library should send receipts automatically for messages
         * By default disabled
         * For the web API, if enabled, the companion won't receive notifications
         *
         * @param automaticMessageReceipts true to enable automatic message receipts, false otherwise
         * @return the same instance for chaining
         */
        public Options automaticMessageReceipts(boolean automaticMessageReceipts) {
            store.setAutomaticMessageReceipts(automaticMessageReceipts);
            return this;
        }

        /**
         * Sets a handler for message previews
         *
         * @param messagePreviewHandler the handler to use, can be null
         * @return the same instance for chaining
         */
        public Options messagePreviewHandler(WhatsappMessagePreviewHandler messagePreviewHandler) {
            this.messagePreviewHandler = messagePreviewHandler;
            return this;
        }

        /**
         * Sets an error handler for the connection
         *
         * @param errorHandler the error handler to use, can be null
         * @return the same instance for chaining
         */
        public Options errorHandler(WhatsappErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public static final class Web extends Options {
            private Web(WhatsappStore store) {
                super(store);
            }

            /**
             * Sets a proxy for the connection
             *
             * @param proxy the proxy to use, can be null to use no proxy
             * @return the same instance for chaining
             */
            @Override
            public Web proxy(URI proxy) {
                return (Web) super.proxy(proxy);
            }

            /**
             * Sets the companion device for the connection
             *
             * @param device the companion device, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web device(JidDevice device) {
                return (Web) super.device(device);
            }

            /**
             * Sets a handler for message previews
             *
             * @param messagePreviewHandler the handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web messagePreviewHandler(WhatsappMessagePreviewHandler messagePreviewHandler) {
                return (Web) super.messagePreviewHandler(messagePreviewHandler);
            }

            /**
             * Sets an error handler for the connection
             *
             * @param errorHandler the error handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web errorHandler(WhatsappErrorHandler errorHandler) {
                return (Web) super.errorHandler(errorHandler);
            }

            /**
             * Controls whether the library should send receipts automatically for messages
             * By default disabled
             * For the web API, if enabled, the companion won't receive notifications
             *
             * @param automaticMessageReceipts true to enable automatic message receipts, false otherwise
             * @return the same instance for chaining
             */
            @Override
            public Web automaticMessageReceipts(boolean automaticMessageReceipts) {
                return (Web) super.automaticMessageReceipts(automaticMessageReceipts);
            }

            /**
             * Sets how much chat history WhatsApp should send when the QR is first scanned
             * By default, one year
             *
             * @param historyLength the history policy to use, must not be null
             * @return the same instance for chaining
             * @throws NullPointerException if historyLength is null
             */
            public Web historySetting(WhatsappWebHistoryPolicy historyLength) {
                Objects.requireNonNull(historyLength, "historyLength must not be null");
                store.setWebHistoryPolicy(historyLength);
                return this;
            }

            /**
             * Creates a WhatsApp instance with a QR code handler
             *
             * @param qrHandler the handler to process QR codes, must not be null
             * @return a non-null WhatsApp instance
             * @throws NullPointerException if qrHandler is null
             */
            public Whatsapp unregistered(WhatsappVerificationHandler.Web.QrCode qrHandler) {
                Objects.requireNonNull(qrHandler, "qrHandler must not be null");
                return new Whatsapp(store, qrHandler, messagePreviewHandler, errorHandler);
            }

            /**
             * Creates a WhatsApp instance with an OTP handler
             *
             * @param phoneNumber the phone value of the user, must be valid
             * @param pairingCodeHandler the handler for the pairing code, must not be null
             * @return a non-null WhatsApp instance
             * @throws NullPointerException if pairingCodeHandler is null
             */
            public Whatsapp unregistered(long phoneNumber, WhatsappVerificationHandler.Web.PairingCode pairingCodeHandler) {
                Objects.requireNonNull(pairingCodeHandler, "pairingCodeHandler must not be null");
                store.setPhoneNumber(phoneNumber);
                return new Whatsapp(store, pairingCodeHandler, messagePreviewHandler, errorHandler);
            }

            /**
             * Creates a WhatsApp instance with no handlers
             * This method assumes that you have already logged in using a QR code or OTP
             * Otherwise, it returns an empty optional.
             *
             * @return an optional containing the WhatsApp instance if registered, empty otherwise
             */
            public Optional<Whatsapp> registered() {
                if (!store.registered()) {
                    return Optional.empty();
                }

                var result = new Whatsapp(store, null, messagePreviewHandler, errorHandler);
                return Optional.of(result);
            }
        }

        public static final class Mobile extends Options {
            private Mobile(WhatsappStore store) {
                super(store);
            }

            /**
             * Sets a proxy for the connection
             *
             * @param proxy the proxy to use, can be null to use no proxy
             * @return the same instance for chaining
             */
            @Override
            public Mobile proxy(URI proxy) {
                store.setProxy(proxy);
                return this;
            }

            /**
             * Sets the companion device for the connection
             *
             * @param device the companion device, can be null
             * @return the same instance for chaining
             */
            @Override
            public Mobile device(JidDevice device) {
                store.setDevice(device);
                return this;
            }

            /**
             * Sets an error handler for the connection
             *
             * @param errorHandler the error handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Mobile errorHandler(WhatsappErrorHandler errorHandler) {
                super.errorHandler(errorHandler);
                return this;
            }

            /**
             * Controls whether the library should send receipts automatically for messages
             * By default disabled
             * For the web API, if enabled, the companion won't receive notifications
             *
             * @param automaticMessageReceipts true to enable automatic message receipts, false otherwise
             * @return the same instance for chaining
             */
            @Override
            public Mobile automaticMessageReceipts(boolean automaticMessageReceipts) {
                super.automaticMessageReceipts(automaticMessageReceipts);
                return this;
            }

            /**
             * Sets the display name for the WhatsApp account
             *
             * @param name the name to set, can be null
             * @return the same instance for chaining
             */
            public Options name(String name) {
                store.setName(name);
                return this;
            }

            /**
             * Sets the about/status message for the WhatsApp account
             *
             * @param about the about message to set, can be null
             * @return the same instance for chaining
             */
            public Options about(String about) {
                store.setAbout(about);
                return this;
            }

            /**
             * Sets the business' address
             *
             * @param businessAddress the address to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessAddress(String businessAddress) {
                store.setBusinessAddress(businessAddress);
                return this;
            }

            /**
             * Sets the business' address longitude
             *
             * @param businessLongitude the longitude to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessLongitude(Double businessLongitude) {
                store.setBusinessLongitude(businessLongitude);
                return this;
            }

            /**
             * Sets the business' address latitude
             *
             * @param businessLatitude the latitude to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessLatitude(Double businessLatitude) {
                store.setBusinessLatitude(businessLatitude);
                return this;
            }

            /**
             * Sets the business' description
             *
             * @param businessDescription the description to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessDescription(String businessDescription) {
                store.setBusinessDescription(businessDescription);
                return this;
            }

            /**
             * Sets the business' website URL
             *
             * @param businessWebsite the website URL to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessWebsite(String businessWebsite) {
                store.setBusinessWebsite(businessWebsite);
                return this;
            }

            /**
             * Sets the business' email address
             *
             * @param businessEmail the email address to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessEmail(String businessEmail) {
                store.setBusinessEmail(businessEmail);
                return this;
            }

            /**
             * Sets the business' category
             *
             * @param businessCategory the category to set, can be null
             * @return the same instance for chaining
             */
            public Mobile businessCategory(BusinessCategory businessCategory) {
                store.setBusinessCategory(businessCategory);
                return this;
            }

            /**
             * Creates a WhatsApp instance assuming the session is already registered
             * This means that the verification code has already been sent to WhatsApp
             *
             * @return an optional containing the WhatsApp instance if registered, empty otherwise
             */
            public Optional<Whatsapp> registered() {
                if (!store.registered()) {
                    return Optional.empty();
                }

                var result = new Whatsapp(store, null, messagePreviewHandler, errorHandler);
                return Optional.of(result);
            }

            /**
             * Creates a WhatsApp instance for a session that needs registration
             * This means that you may or may not have a verification code, but it hasn't been sent to WhatsApp yet
             *
             * @param phoneNumber the phone value to register, must be valid
             * @param verification the verification handler to use, must not be null
             * @return a non-null WhatsApp instance
             * @throws NullPointerException if verification is null
             * @throws IllegalArgumentException if the store already has a phone number set, and the phone number is different from the one being registered
             */
            public Whatsapp register(long phoneNumber, WhatsappVerificationHandler.Mobile verification) {
                Objects.requireNonNull(verification, "verification must not be null");

                var oldPhoneNumber = store.phoneNumber();
                if(oldPhoneNumber.isPresent() && oldPhoneNumber.getAsLong() != phoneNumber) {
                    throw new IllegalArgumentException("The phone number(" + phoneNumber + ") must match the existing phone number(" + oldPhoneNumber.getAsLong() + ")");
                }else {
                    store.setPhoneNumber(phoneNumber);
                }

                if (!store.registered()) {
                    try(var activator = new WhatsappMobileClientRegistration(store, verification)) {
                        activator.register();
                    }
                }

                return new Whatsapp(store, null, messagePreviewHandler, errorHandler);
            }
        }
    }

    public static final class Custom extends WhatsappBuilder {
        private static final WhatsappVerificationHandler.Web DEFAULT_WEB_VERIFICATION_HANDLER = WhatsappVerificationHandler.Web.QrCode.toTerminal();
        private static final WhatsappMessagePreviewHandler DEFAULT_MESSAGE_PREVIEW_HANDLER = WhatsappMessagePreviewHandler.enabled(true);
        private static final WhatsappErrorHandler DEFAULT_ERROR_HANDLER = WhatsappErrorHandler.toTerminal();

        private WhatsappStore store;
        private WhatsappMessagePreviewHandler messagePreviewHandler;
        private WhatsappErrorHandler errorHandler;
        private WhatsappVerificationHandler.Web webVerificationHandler;

        private Custom() {

        }

        /**
         * Sets the store for the connection
         *
         * @param store the store to use, can be null
         * @return the same instance for chaining
         */
        public Custom store(WhatsappStore store) {
            this.store = store;
            return this;
        }

        /**
         * Sets an error handler for the connection
         *
         * @param errorHandler the error handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom errorHandler(WhatsappErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets the web verification handler for the connection
         *
         * @param webVerificationHandler the verification handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom webVerificationSupport(WhatsappVerificationHandler.Web webVerificationHandler) {
            this.webVerificationHandler = webVerificationHandler;
            return this;
        }

        /**
         * Sets a message preview handler for the connection
         *
         * @param messagePreviewHandler the handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom messagePreviewHandler(WhatsappMessagePreviewHandler messagePreviewHandler) {
            this.messagePreviewHandler = messagePreviewHandler;
            return this;
        }

        /**
         * Builds a WhatsApp instance with the configured parameters
         *
         * @return a non-null WhatsApp instance
         * @throws NullPointerException if store or keys are null
         * @throws IllegalArgumentException if there is a UUID mismatch between store and keys
         */
        public Whatsapp build() {
            var store = Objects.requireNonNull(this.store, "Expected a valid store");
            var webVerificationHandler = switch (store.clientType()) {
                case WEB -> Objects.requireNonNullElse(this.webVerificationHandler, DEFAULT_WEB_VERIFICATION_HANDLER);
                case MOBILE -> null;
            };
            var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
            var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
            return new Whatsapp(store, webVerificationHandler, messagePreviewHandler, errorHandler);
        }
    }
}