package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.*;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.jid.JidDevice;
import it.auties.whatsapp.util.MobileRegistration;

import java.net.URI;
import java.util.*;

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
        public abstract Options newConnection();

        /**
         * Creates a new connection from the first connection that was serialized
         * If no connection is available, a new one will be created
         *
         * @return a non-null options selector
         */
        public abstract Options firstConnection();

        /**
         * Creates a new connection from the last connection that was serialized
         * If no connection is available, a new one will be created
         *
         * @return a non-null options selector
         */
        public abstract Options lastConnection();

        /**
         * Creates a new connection using a unique identifier
         * If a session with the given id already exists, it will be retrieved.
         * Otherwise, a new one will be created.
         *
         * @param uuid the UUID to use to create the connection, can be null (will generate a random UUID)
         * @return a non-null options selector
         */
        public abstract Options newConnection(UUID uuid);

        /**
         * Creates a new connection using a phone value
         * If a session with the given phone value already exists, it will be retrieved.
         * Otherwise, a new one will be created.
         *
         * @param phoneNumber the phone value to use to create the connection, can be null (will generate a random UUID)
         * @return a non-null options selector
         */
        public abstract Options newConnection(Long phoneNumber);

        /**
         * Creates a new connection using a six parts key representation
         *
         * @param sixParts the six parts keys to use to create the connection, must not be null
         * @return a non-null options selector
         * @throws NullPointerException if sixParts is null
         */
        public abstract Options newConnection(WhatsappSixPartsKeys sixParts);

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
            public Options.Web newConnection() {
                return newConnection(UUID.randomUUID());
            }

            /**
             * Creates a new connection from the first connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Web firstConnection() {
                var uuids = serializer.listIds(WhatsappClientType.WEB);
                if(uuids.isEmpty()) {
                    return newConnection();
                }else {
                    return newConnection(uuids.getFirst());
                }
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Web lastConnection() {
                var uuids = serializer.listIds(WhatsappClientType.WEB);
                if(uuids.isEmpty()) {
                    return newConnection();
                }else {
                    return newConnection(uuids.getLast());
                }
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
            public Options.Web newConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(sessionUuid, null, WhatsappClientType.WEB)
                        .orElseGet(() -> serializer.newStoreKeysPair(sessionUuid, null, WhatsappClientType.WEB));
                return createConnection(sessionStoreAndKeys);
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
            public Options.Web newConnection(Long phoneNumber) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, phoneNumber, WhatsappClientType.WEB)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), phoneNumber, WhatsappClientType.WEB));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the six parts keys to use to create the connection, must not be null
             * @return a non-null options selector
             * @throws NullPointerException if sixParts is null
             */
            @Override
            public Options.Web newConnection(WhatsappSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.deserializeStoreKeysPair(null, sixParts.phoneNumber(), WhatsappClientType.WEB);
                if(serialized.isPresent()) {
                    return createConnection(serialized.get());
                }

                var uuid = UUID.randomUUID();
                var keys = new KeysBuilder()
                        .uuid(uuid)
                        .phoneNumber(sixParts.phoneNumber())
                        .noiseKeyPair(sixParts.noiseKeyPair())
                        .identityKeyPair(sixParts.identityKeyPair())
                        .identityId(sixParts.identityId())
                        .registered(true)
                        .clientType(WhatsappClientType.WEB)
                        .build();
                keys.setSerializer(serializer);
                var phoneNumber = keys.phoneNumber();
                var store = WhatsappStore.of(uuid, phoneNumber.isEmpty() ? null : phoneNumber.getAsLong(), WhatsappClientType.WEB);
                store.setSerializer(serializer);
                return createConnection(new StoreKeysPair(store, keys));
            }

            private Options.Web createConnection(StoreKeysPair sessionStoreAndKeys) {
                Objects.requireNonNull(sessionStoreAndKeys, "sessionStoreAndKeys must not be null");
                return new Options.Web(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
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
            public Options.Mobile newConnection() {
                return newConnection(UUID.randomUUID());
            }

            /**
             * Creates a new connection from the first connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile firstConnection() {
                var uuids = serializer.listIds(WhatsappClientType.MOBILE);
                if(uuids.isEmpty()) {
                    return newConnection();
                }else {
                    return newConnection(uuids.getFirst());
                }
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile lastConnection() {
                var uuids = serializer.listIds(WhatsappClientType.MOBILE);
                if(uuids.isEmpty()) {
                    return newConnection();
                }else {
                    return newConnection(uuids.getLast());
                }
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
            public Options.Mobile newConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(sessionUuid, null, WhatsappClientType.MOBILE)
                        .orElseGet(() -> serializer.newStoreKeysPair(sessionUuid, null, WhatsappClientType.MOBILE));
                return createConnection(sessionStoreAndKeys);
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
            public Options.Mobile newConnection(Long phoneNumber) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, phoneNumber, WhatsappClientType.MOBILE)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), phoneNumber, WhatsappClientType.MOBILE));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the six parts keys to use to create the connection, must not be null
             * @return a non-null options selector
             * @throws NullPointerException if sixParts is null
             */
            @Override
            public Options.Mobile newConnection(WhatsappSixPartsKeys sixParts) {
                Objects.requireNonNull(sixParts, "sixParts must not be null");

                var serialized = serializer.deserializeStoreKeysPair(null, sixParts.phoneNumber(), WhatsappClientType.MOBILE);
                if(serialized.isPresent()) {
                    return createConnection(serialized.get());
                }

                var uuid = UUID.randomUUID();
                var keys = new KeysBuilder()
                        .uuid(uuid)
                        .phoneNumber(sixParts.phoneNumber())
                        .noiseKeyPair(sixParts.noiseKeyPair())
                        .identityKeyPair(sixParts.identityKeyPair())
                        .identityId(sixParts.identityId())
                        .registered(true)
                        .clientType(WhatsappClientType.MOBILE)
                        .build();
                keys.setSerializer(serializer);
                var phoneNumber = keys.phoneNumber();
                var store = WhatsappStore.of(uuid, phoneNumber.isEmpty() ? null : phoneNumber.getAsLong(), WhatsappClientType.MOBILE);
                store.setSerializer(serializer);
                return createConnection(new StoreKeysPair(store, keys));
            }

            private Options.Mobile createConnection(StoreKeysPair sessionStoreAndKeys) {
                Objects.requireNonNull(sessionStoreAndKeys, "sessionStoreAndKeys must not be null");
                return new Options.Mobile(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
            }
        }
    }

    public static sealed class Options extends WhatsappBuilder {
        final WhatsappStore store;
        final Keys keys;
        WhatsappMessagePreviewHandler messagePreviewHandler;
        WhatsappErrorHandler errorHandler;

        private Options(WhatsappStore store, Keys keys) {
            this.store = Objects.requireNonNull(store, "store must not be null");
            this.keys = Objects.requireNonNull(keys, "keys must not be null");
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
            private Web(WhatsappStore store, Keys keys) {
                super(store, keys);
            }

            /**
             * Sets a proxy for the connection
             *
             * @param proxy the proxy to use, can be null to use no proxy
             * @return the same instance for chaining
             */
            @Override
            public Web proxy(URI proxy) {
                super.proxy(proxy);
                return this;
            }

            /**
             * Sets the companion device for the connection
             *
             * @param device the companion device, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web device(JidDevice device) {
                super.device(device);
                return this;
            }

            /**
             * Sets a handler for message previews
             *
             * @param messagePreviewHandler the handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web messagePreviewHandler(WhatsappMessagePreviewHandler messagePreviewHandler) {
                super.messagePreviewHandler(messagePreviewHandler);
                return this;
            }

            /**
             * Sets an error handler for the connection
             *
             * @param errorHandler the error handler to use, can be null
             * @return the same instance for chaining
             */
            @Override
            public Web errorHandler(WhatsappErrorHandler errorHandler) {
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
            public Web automaticMessageReceipts(boolean automaticMessageReceipts) {
                super.automaticMessageReceipts(automaticMessageReceipts);
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
            public Web historySetting(WhatsappWebHistoryPolicy historyLength) {
                Objects.requireNonNull(historyLength, "historyLength must not be null");
                store.setWebHistorySetting(historyLength);
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
                return new Whatsapp(store, keys, qrHandler, messagePreviewHandler, errorHandler);
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
                return new Whatsapp(store, keys, pairingCodeHandler, messagePreviewHandler, errorHandler);
            }

            /**
             * Creates a WhatsApp instance with no handlers
             * This method assumes that you have already logged in using a QR code or OTP
             * Otherwise, it returns an empty optional.
             *
             * @return an optional containing the WhatsApp instance if registered, empty otherwise
             */
            public Optional<Whatsapp> registered() {
                if (!keys.registered()) {
                    return Optional.empty();
                }

                var result = new Whatsapp(store, keys, null, messagePreviewHandler, errorHandler);
                return Optional.of(result);
            }
        }

        public static final class Mobile extends Options {
            private Mobile(WhatsappStore store, Keys keys) {
                super(store, keys);
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
                if (!keys.registered()) {
                    return Optional.empty();
                }

                var result = new Whatsapp(store, keys, null, messagePreviewHandler, errorHandler);
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
             */
            public Whatsapp register(long phoneNumber, WhatsappVerificationHandler.Mobile verification) {
                Objects.requireNonNull(verification, "verification must not be null");

                if (!keys.registered()) {
                    keys.setPhoneNumber(phoneNumber);
                    store.setPhoneNumber(phoneNumber);
                    try(var registration = new MobileRegistration(store, keys, verification)) {
                        registration.register();
                    }
                }

                return new Whatsapp(store, keys, null, messagePreviewHandler, errorHandler);
            }
        }
    }

    public static final class Custom extends WhatsappBuilder {
        private WhatsappStore store;
        private Keys keys;
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
         * Sets the keys for the connection
         *
         * @param keys the keys to use, can be null
         * @return the same instance for chaining
         */
        public Custom keys(Keys keys) {
            this.keys = keys;
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
            Objects.requireNonNull(store, "Expected a valid store");
            Objects.requireNonNull(keys, "Expected a valid keys");
            if (!Objects.equals(store.uuid(), keys.uuid())) {
                throw new IllegalArgumentException("UUID mismatch: %s != %s".formatted(store.uuid(), keys.uuid()));
            }
            return new Whatsapp(
                    store,
                    keys,
                    getWebVerificationMethod(store, webVerificationHandler),
                    messagePreviewHandler,
                    Objects.requireNonNullElse(errorHandler, WhatsappErrorHandler.toTerminal()));
        }

        private static WhatsappVerificationHandler.Web getWebVerificationMethod(WhatsappStore store, WhatsappVerificationHandler.Web webVerificationHandler) {
            Objects.requireNonNull(store, "store must not be null");
            return switch (store.clientType()) {
                case WEB -> Objects.requireNonNullElse(webVerificationHandler, WhatsappVerificationHandler.Web.QrCode.toTerminal());
                case MOBILE -> null;
            };
        }
    }
}