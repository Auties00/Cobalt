package com.github.auties00.cobalt.client;

import com.github.auties00.cobalt.client.registration.WhatsAppMobileClientRegistration;
import com.github.auties00.cobalt.model.auth.Version;
import com.github.auties00.cobalt.model.business.BusinessCategory;
import com.github.auties00.cobalt.model.jid.JidCompanion;
import com.github.auties00.cobalt.store.WhatsAppStore;
import com.github.auties00.cobalt.store.WhatsAppStoreBuilder;
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

public sealed class WhatsAppClientBuilder {
    private static final WhatsAppClientMessagePreviewHandler DEFAULT_MESSAGE_PREVIEW_HANDLER = WhatsAppClientMessagePreviewHandler.enabled(true);
    private static final WhatsAppClientErrorHandler DEFAULT_ERROR_HANDLER = WhatsAppClientErrorHandler.toTerminal();
    private static final WhatsAppClientVerificationHandler.Web DEFAULT_WEB_VERIFICATION_HANDLER = WhatsAppClientVerificationHandler.Web.QrCode.toTerminal();
    
    static final WhatsAppClientBuilder INSTANCE = new WhatsAppClientBuilder();

    private WhatsAppClientBuilder() {

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

    public static abstract sealed class Client extends WhatsAppClientBuilder {
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
        public abstract Options loadConnection(WhatsAppClientSixPartsKeys sixParts);

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

        WhatsAppStore newStore(UUID id, Long phoneNumber, WhatsAppClientType clientType, SignalIdentityKeyPair identityKeyPair, SignalIdentityKeyPair noiseKeyPair, boolean registered, byte[] identityId) {
            var device = switch (clientType) {
                case WEB -> JidCompanion.web();
                case MOBILE -> JidCompanion.ios(false);
            };
            var result = new WhatsAppStoreBuilder()
                    .uuid(Objects.requireNonNullElseGet(id, UUID::randomUUID))
                    .phoneNumber(phoneNumber)
                    .clientType(Objects.requireNonNull(clientType, "clientType must not be null"))
                    .device(device)
                    .identityId(identityId)
                    .identityKeyPair(identityKeyPair)
                    .noiseKeyPair(noiseKeyPair)
                    .registered(registered)
                    .build();
            result.setSerializable(true);
            result.setSerializer(serializer);
            return result;
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
                var uuids = serializer.listIds(WhatsAppClientType.WEB);
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

                return serializer.startDeserialize(WhatsAppClientType.WEB, uuid, null)
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
                var store = serializer.startDeserialize(WhatsAppClientType.WEB, sessionUuid, null)
                        .orElseGet(() -> newStore(sessionUuid, null, WhatsAppClientType.WEB, null, null, false, null));
                return new Options.Web(store);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) {
                if (phoneNumber == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsAppClientType.WEB, null, phoneNumber)
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
                var store = serializer.startDeserialize(WhatsAppClientType.WEB, null, phoneNumber)
                        .orElseGet(() -> newStore(null, phoneNumber, WhatsAppClientType.WEB, null, null, false, null));
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
            public Options.Web loadConnection(WhatsAppClientSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.startDeserialize(WhatsAppClientType.WEB, null, sixParts.phoneNumber());
                if(serialized.isPresent()) {
                    return new Options.Web(serialized.get());
                }

                var store = newStore(null, sixParts.phoneNumber(), WhatsAppClientType.WEB, sixParts.identityKeyPair(), sixParts.noiseKeyPair(), true, sixParts.identityId());
                return new Options.Web(store);
            }

            @Override
            public Optional<Options> loadLastConnection() {
                var uuids = serializer.listIds(WhatsAppClientType.WEB);
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
                var uuids = serializer.listIds(WhatsAppClientType.MOBILE);
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

                return serializer.startDeserialize(WhatsAppClientType.MOBILE, uuid, null)
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
                var store = serializer.startDeserialize(WhatsAppClientType.MOBILE, sessionUuid, null)
                        .orElseGet(() -> newStore(sessionUuid, null, WhatsAppClientType.MOBILE, null, null, false, null));
                return new Options.Mobile(store);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) {
                if(phoneNumber == null) {
                    return Optional.empty();
                }

                return serializer.startDeserialize(WhatsAppClientType.MOBILE, null, phoneNumber)
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
                var store = serializer.startDeserialize(WhatsAppClientType.MOBILE, null, phoneNumber)
                        .orElseGet(() -> newStore(null, phoneNumber, WhatsAppClientType.MOBILE, null, null, false, null));
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
            public Options.Mobile loadConnection(WhatsAppClientSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.startDeserialize(WhatsAppClientType.MOBILE, null, sixParts.phoneNumber());
                if(serialized.isPresent()) {
                    return new Options.Mobile(serialized.get());
                }

                var store = newStore(null, sixParts.phoneNumber(), WhatsAppClientType.MOBILE, sixParts.identityKeyPair(), sixParts.noiseKeyPair(), true, sixParts.identityId());
                return new Options.Mobile(store);
            }

            @Override
            public Optional<Options> loadLastConnection() {
                var uuids = serializer.listIds(WhatsAppClientType.MOBILE);
                if(uuids.isEmpty()) {
                    return Optional.empty();
                }else {
                    return loadConnection(uuids.getLast());
                }
            }
        }
    }

    public static sealed class Options extends WhatsAppClientBuilder {
        final WhatsAppStore store;
        WhatsAppClientMessagePreviewHandler messagePreviewHandler;
        WhatsAppClientErrorHandler errorHandler;

        private Options(WhatsAppStore store) {
            this.store = Objects.requireNonNull(store, "store must not be null");
        }

        /**
         * Sets the display name
         * On Mobile, this is the preferred name that contacts that haven't saved you yet see next to your phone number.
         * On Web, this is the name of the companion device, visible in the "Linked Devices" tab
         *
         * @param name the name to set, can be null
         * @return the same instance for chaining
         */
        public Options name(String name) {
            store.setName(name);
            return this;
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
        public Options device(JidCompanion device) {
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
         * Sets the client version for the connection
         * This allows customization of the WhatsApp client version identifier
         *
         * @param clientVersion the client version to use, can be null to use the default
         * @return the same instance for chaining
         */
        public Options clientVersion(Version clientVersion) {
            store.setClientVersion(clientVersion);
            return this;
        }

        /**
         * Sets a handler for message previews
         *
         * @param messagePreviewHandler the handler to use, can be null
         * @return the same instance for chaining
         */
        public Options messagePreviewHandler(WhatsAppClientMessagePreviewHandler messagePreviewHandler) {
            this.messagePreviewHandler = messagePreviewHandler;
            return this;
        }

        /**
         * Sets an error handler for the connection
         *
         * @param errorHandler the error handler to use, can be null
         * @return the same instance for chaining
         */
        public Options errorHandler(WhatsAppClientErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public static final class Web extends Options {
            private Web(WhatsAppStore store) {
                super(store);
            }

            /**
             * Sets the display name for the companion device, visible in the "Linked Devices" tab
             *
             * @param name the name to set, can be null
             * @return the same instance for chaining
             */
            @Override
            public Mobile name(String name) {
                return (Mobile) super.name(name);
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
            public Web device(JidCompanion device) {
                return (Web) super.device(device);
            }

            /**
             * Sets a handler for message previews
             *
             * @param messagePreviewHandler the handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web messagePreviewHandler(WhatsAppClientMessagePreviewHandler messagePreviewHandler) {
                return (Web) super.messagePreviewHandler(messagePreviewHandler);
            }

            /**
             * Sets an error handler for the connection
             *
             * @param errorHandler the error handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web errorHandler(WhatsAppClientErrorHandler errorHandler) {
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
             * Sets the client version for the connection
             * This allows customization of the WhatsApp client version identifier
             *
             * @param clientVersion the client version to use, can be null to use the default
             * @return the same instance for chaining
             */
            @Override
            public Web clientVersion(Version clientVersion) {
                return (Web) super.clientVersion(clientVersion);
            }

            /**
             * Sets the display name for the WhatsApp account
             *
             * @param name the name to set, can be null
             * @return the same instance for chaining
             */
            public Web name(String name) {
                store.setName(name);
                return this;
            }

            /**
             * Sets how much chat history WhatsApp should send when the QR is first scanned
             * By default, one year
             *
             * @param historyLength the history policy to use, must not be null
             * @return the same instance for chaining
             * @throws NullPointerException if historyLength is null
             */
            public Web historySetting(WhatsAppWebClientHistory historyLength) {
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
            public WhatsAppClient unregistered(WhatsAppClientVerificationHandler.Web.QrCode qrHandler) {
                Objects.requireNonNull(qrHandler, "qrHandler must not be null");
                var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new WhatsAppClient(store, qrHandler, messagePreviewHandler, errorHandler);
            }

            /**
             * Creates a WhatsApp instance with an OTP handler
             *
             * @param phoneNumber the phone value of the user, must be valid
             * @param pairingCodeHandler the handler for the pairing code, must not be null
             * @return a non-null WhatsApp instance
             * @throws NullPointerException if pairingCodeHandler is null
             */
            public WhatsAppClient unregistered(long phoneNumber, WhatsAppClientVerificationHandler.Web.PairingCode pairingCodeHandler) {
                Objects.requireNonNull(pairingCodeHandler, "pairingCodeHandler must not be null");
                store.setPhoneNumber(phoneNumber);
                var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new WhatsAppClient(store, pairingCodeHandler, messagePreviewHandler, errorHandler);
            }

            /**
             * Creates a WhatsApp instance with no handlers
             * This method assumes that you have already logged in using a QR code or OTP
             * Otherwise, it returns an empty optional.
             *
             * @return an optional containing the WhatsApp instance if registered, empty otherwise
             */
            public Optional<WhatsAppClient> registered() {
                if (!store.registered()) {
                    return Optional.empty();
                }

                var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                var result = new WhatsAppClient(store, null, messagePreviewHandler, errorHandler);
                return Optional.of(result);
            }
        }

        public static final class Mobile extends Options {
            private Mobile(WhatsAppStore store) {
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
            public Mobile device(JidCompanion device) {
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
            public Mobile errorHandler(WhatsAppClientErrorHandler errorHandler) {
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
             * Sets the client version for the connection
             * This allows customization of the WhatsApp client version identifier
             *
             * @param clientVersion the client version to use, can be null to use the default
             * @return the same instance for chaining
             */
            @Override
            public Mobile clientVersion(Version clientVersion) {
                return (Mobile) super.clientVersion(clientVersion);
            }

            /**
             * Sets the display name for the WhatsApp account
             * This is the preferred name that contacts that haven't saved you yet see next to your phone number.
             *
             * @param name the name to set, can be null
             * @return the same instance for chaining
             */
            @Override
            public Mobile name(String name) {
                return (Mobile) super.name(name);
            }

            /**
             * Sets the about/status message for the WhatsApp account
             *
             * @param about the about message to set, can be null
             * @return the same instance for chaining
             */
            public Mobile about(String about) {
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
            public Optional<WhatsAppClient> registered() {
                if (!store.registered()) {
                    return Optional.empty();
                }

                var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                var result = new WhatsAppClient(store, null, messagePreviewHandler, errorHandler);
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
            public WhatsAppClient register(long phoneNumber, WhatsAppClientVerificationHandler.Mobile verification) {
                Objects.requireNonNull(verification, "verification must not be null");

                var oldPhoneNumber = store.phoneNumber();
                if(oldPhoneNumber.isPresent() && oldPhoneNumber.getAsLong() != phoneNumber) {
                    throw new IllegalArgumentException("The phone number(" + phoneNumber + ") must match the existing phone number(" + oldPhoneNumber.getAsLong() + ")");
                }else {
                    store.setPhoneNumber(phoneNumber);
                }

                if (!store.registered()) {
                    try(var registration = WhatsAppMobileClientRegistration.of(store, verification)) {
                        registration.register();
                    }
                }

                var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new WhatsAppClient(store, null, messagePreviewHandler, errorHandler);
            }
        }
    }

    public static final class Custom extends WhatsAppClientBuilder {
        private WhatsAppStore store;
        private WhatsAppClientMessagePreviewHandler messagePreviewHandler;
        private WhatsAppClientErrorHandler errorHandler;
        private WhatsAppClientVerificationHandler.Web webVerificationHandler;

        private Custom() {

        }

        /**
         * Sets the store for the connection
         *
         * @param store the store to use, can be null
         * @return the same instance for chaining
         */
        public Custom store(WhatsAppStore store) {
            this.store = store;
            return this;
        }

        /**
         * Sets an error handler for the connection
         *
         * @param errorHandler the error handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom errorHandler(WhatsAppClientErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets the web verification handler for the connection
         *
         * @param webVerificationHandler the verification handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom webVerificationSupport(WhatsAppClientVerificationHandler.Web webVerificationHandler) {
            this.webVerificationHandler = webVerificationHandler;
            return this;
        }

        /**
         * Sets a message preview handler for the connection
         *
         * @param messagePreviewHandler the handler to use, can be null
         * @return the same instance for chaining
         */
        public Custom messagePreviewHandler(WhatsAppClientMessagePreviewHandler messagePreviewHandler) {
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
        public WhatsAppClient build() {
            var store = Objects.requireNonNull(this.store, "Expected a valid store");
            var webVerificationHandler = switch (store.clientType()) {
                case WEB -> Objects.requireNonNullElse(this.webVerificationHandler, DEFAULT_WEB_VERIFICATION_HANDLER);
                case MOBILE -> null;
            };
            var messagePreviewHandler = Objects.requireNonNullElse(this.messagePreviewHandler, DEFAULT_MESSAGE_PREVIEW_HANDLER);
            var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
            return new WhatsAppClient(store, webVerificationHandler, messagePreviewHandler, errorHandler);
        }
    }
}
