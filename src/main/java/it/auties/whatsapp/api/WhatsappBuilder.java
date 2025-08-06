package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.*;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.SixPartsKeys;
import it.auties.whatsapp.util.MobileRegistration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public sealed class WhatsappBuilder {
    static final WhatsappBuilder INSTANCE = new WhatsappBuilder();

    private WhatsappBuilder() {

    }

    public Client.Web webClient() {
        return new Client.Web();
    }

    public Client.Mobile mobileClient() {
        return new Client.Mobile();
    }

    public Custom customClient() {
        return new Custom();
    }

    public static abstract sealed class Client extends WhatsappBuilder {
        ControllerSerializer serializer;

        private Client() {
            this.serializer = ControllerSerializer.toProtobuf();
        }

        public Client serializer(ControllerSerializer serializer) {
            Objects.requireNonNull(serializer, "Serializer cannot be null");
            this.serializer = serializer;
            return this;
        }

        /**
         * Creates a new connection using a random uuid
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
         * If a session with the given id already otpEligible, it will be retrieved.
         * Otherwise, a new one will be created.
         *
         * @param uuid the nullable uuid to use to create the connection
         * @return a non-null options selector
         */
        public abstract Options newConnection(UUID uuid);
        
        /**
         * Creates a new connection using a phone number
         * If a session with the given phone number already exists, it will be retrieved.
         * Otherwise, a new one will be created.
         *
         * @param phoneNumber the nullable phone number to use to create the connection
         * @return a non-null options selector
         */
        public abstract Options newConnection(PhoneNumber phoneNumber);
        /**
         * Creates a new connection using an alias
         * If a session with the given alias already exists, it will be retrieved.
         * Otherwise, a new one will be created.
         *
         * @param alias the nullable alias to use to create the connection
         * @return a non-null options selector
         */
        public abstract Options newConnection(String alias);
        
        /**
         * Creates a new connection using a six parts key representation
         *
         * @param sixParts the non-null six parts to use to create the connection
         * @return a non-null options selector
         */
        public abstract Options newConnection(SixPartsKeys sixParts);
        
        public static final class Web extends Client {
            private Web() {

            }

            @Override
            public Web serializer(ControllerSerializer serializer) {
                this.serializer = serializer;
                return this;
            }

            /**
             * Creates a new connection using a random uuid
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
                return newConnection(serializer.listIds(WhatsappClientType.WEB).peekFirst());
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Web lastConnection() {
                return newConnection(serializer.listIds(WhatsappClientType.WEB).peekLast());
            }

            /**
             * Creates a new connection using a unique identifier
             * If a session with the given id already otpEligible, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param uuid the nullable uuid to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Web newConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(sessionUuid, null, null, WhatsappClientType.WEB)
                        .orElseGet(() -> serializer.newStoreKeysPair(sessionUuid, null, null, WhatsappClientType.WEB));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a phone number
             * If a session with the given phone number already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param phoneNumber the nullable phone number to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Web newConnection(PhoneNumber phoneNumber) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, phoneNumber, null, WhatsappClientType.WEB)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), phoneNumber, null, WhatsappClientType.WEB));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using an alias
             * If a session with the given alias already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param alias the nullable alias to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Web newConnection(String alias) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, null, alias, WhatsappClientType.WEB)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), null, alias != null ? List.of(alias) : null, WhatsappClientType.WEB));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the non-null six parts to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Web newConnection(SixPartsKeys sixParts) {
                var serialized = serializer.deserializeStoreKeysPair(null, sixParts.phoneNumber(), null, WhatsappClientType.MOBILE);
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
                var phoneNumber = keys.phoneNumber()
                        .orElse(null);
                var store = Store.of(uuid, phoneNumber, null, WhatsappClientType.MOBILE);
                store.setSerializer(serializer);
                return createConnection(new StoreKeysPair(store, keys));
            }

            private Options.Web createConnection(StoreKeysPair sessionStoreAndKeys) {
                return new Options.Web(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
            }
        }

        public static final class Mobile extends Client {
            private Mobile() {

            }

            @Override
            public Mobile serializer(ControllerSerializer serializer) {
                this.serializer = serializer;
                return this;
            }

            /**
             * Creates a new connection using a random uuid
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
                return newConnection(serializer.listIds(WhatsappClientType.MOBILE).peekFirst());
            }

            /**
             * Creates a new connection from the last connection that was serialized
             * If no connection is available, a new one will be created
             *
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile lastConnection() {
                return newConnection(serializer.listIds(WhatsappClientType.MOBILE).peekLast());
            }

            /**
             * Creates a new connection using a unique identifier
             * If a session with the given id already otpEligible, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param uuid the nullable uuid to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile newConnection(UUID uuid) {
                var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(sessionUuid, null, null, WhatsappClientType.MOBILE)
                        .orElseGet(() -> serializer.newStoreKeysPair(sessionUuid, null, null, WhatsappClientType.MOBILE));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a phone number
             * If a session with the given phone number already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param phoneNumber the nullable phone number to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile newConnection(PhoneNumber phoneNumber) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, phoneNumber, null, WhatsappClientType.MOBILE)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), phoneNumber, null, WhatsappClientType.MOBILE));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using an alias
             * If a session with the given alias already exists, it will be retrieved.
             * Otherwise, a new one will be created.
             *
             * @param alias the nullable alias to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile newConnection(String alias) {
                var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, null, alias, WhatsappClientType.MOBILE)
                        .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), null, alias != null ? List.of(alias) : null, WhatsappClientType.MOBILE));
                return createConnection(sessionStoreAndKeys);
            }

            /**
             * Creates a new connection using a six parts key representation
             *
             * @param sixParts the non-null six parts to use to create the connection
             * @return a non-null options selector
             */
            @Override
            public Options.Mobile newConnection(SixPartsKeys sixParts) {
                var serialized = serializer.deserializeStoreKeysPair(null, sixParts.phoneNumber(), null, WhatsappClientType.MOBILE);
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
                var phoneNumber = keys.phoneNumber()
                        .orElse(null);
                var store = Store.of(uuid, phoneNumber, null, WhatsappClientType.MOBILE);
                store.setSerializer(serializer);
                return createConnection(new StoreKeysPair(store, keys));
            }

            private Options.Mobile createConnection(StoreKeysPair sessionStoreAndKeys) {
                return new Options.Mobile(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
            }
        }
    }
    
    public static sealed class Options extends WhatsappBuilder {
        final Store store;
        final Keys keys;
        WhatsappErrorHandler errorHandler;

        private Options(Store store, Keys keys) {
            this.store = store;
            this.keys = keys;
        }

        public Options errorHandler(WhatsappErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public static final class Web extends Options {
            private Whatsapp whatsapp;

            private Web(Store store, Keys keys) {
                super(store, keys);
            }

            @Override
            public Web errorHandler(WhatsappErrorHandler errorHandler) {
                super.errorHandler(errorHandler);
                return this;
            }

            /**
             * Whether the library should send receipts automatically for messages
             * By default disabled
             * For the web api, if enabled, the companion won't receive notifications
             *
             * @return the same instance for chaining
             */
            public Web automaticMessageReceipts(boolean automaticMessageReceipts) {
                store.setAutomaticMessageReceipts(automaticMessageReceipts);
                return this;
            }

            /**
             * Sets how much chat history Whatsapp should send when the QR is first scanned.
             * By default, one year
             *
             * @return the same instance for chaining
             */
            public Web historySetting(WhatsappWebHistoryPolicy historyLength) {
                store.setWebHistorySetting(historyLength);
                return this;
            }

            /**
             * Creates a Whatsapp instance with a qr handler
             *
             * @param qrHandler the non-null handler to use
             * @return a Whatsapp instance
             */
            public Whatsapp unregistered(WhatsappVerificationHandler.Web.QrCode qrHandler) {
                if (whatsapp == null) {
                    this.whatsapp = Whatsapp.builder()
                            .customClient()
                            .store(store)
                            .keys(keys)
                            .errorHandler(errorHandler)
                            .webVerificationSupport(qrHandler)
                            .build();
                }

                return whatsapp;
            }

            /**
             * Creates a Whatsapp instance with an OTP handler
             *
             * @param phoneNumber        the phone number of the user
             * @param pairingCodeHandler the non-null handler for the pairing code
             * @return a Whatsapp instance
             */
            public Whatsapp unregistered(long phoneNumber, WhatsappVerificationHandler.Web.PairingCode pairingCodeHandler) {
                if (whatsapp == null) {
                    var parsedPhoneNumber = PhoneNumber.of(phoneNumber)
                            .orElseThrow(() -> new IllegalArgumentException(phoneNumber + " is not a valid phone number"));
                    store.setPhoneNumber(parsedPhoneNumber);
                    this.whatsapp = Whatsapp.builder()
                            .customClient()
                            .store(store)
                            .keys(keys)
                            .errorHandler(errorHandler)
                            .webVerificationSupport(pairingCodeHandler)
                            .build();
                }

                return whatsapp;
            }

            /**
             * Creates a Whatsapp instance with no handlers
             * This method assumes that you have already logged in using a QR code or OTP
             * Otherwise, it returns an empty optional.
             *
             * @return an optional
             */
            public Optional<Whatsapp> registered() {
                if (!keys.registered()) {
                    return Optional.empty();
                }

                if (whatsapp == null) {
                    this.whatsapp = Whatsapp.builder()
                            .customClient()
                            .store(store)
                            .keys(keys)
                            .errorHandler(errorHandler)
                            .build();
                }

                return Optional.of(whatsapp);
            }
        }
        
        public static final class Mobile extends Options {
            private Mobile(Store store, Keys keys) {
                super(store, keys);
            }

            @Override
            public Mobile errorHandler(WhatsappErrorHandler errorHandler) {
                super.errorHandler(errorHandler);
                return this;
            }

            /**
             * Set the device to emulate
             *
             * @return the same instance for chaining
             */
            public Mobile device(CompanionDevice device) {
                store.setDevice(device);
                return this;
            }

            /**
             * Sets the business' address
             *
             * @return the same instance for chaining
             */
            public Mobile businessAddress(String businessAddress) {
                store.setBusinessAddress(businessAddress);
                return this;
            }

            /**
             * Sets the business' address longitude
             *
             * @return the same instance for chaining
             */
            public Mobile businessLongitude(Double businessLongitude) {
                store.setBusinessLongitude(businessLongitude);
                return this;
            }

            /**
             * Sets the business' address latitude
             *
             * @return the same instance for chaining
             */
            public Mobile businessLatitude(Double businessLatitude) {
                store.setBusinessLatitude(businessLatitude);
                return this;
            }

            /**
             * Sets the business' description
             *
             * @return the same instance for chaining
             */
            public Mobile businessDescription(String businessDescription) {
                store.setBusinessDescription(businessDescription);
                return this;
            }

            /**
             * Sets the business' website
             *
             * @return the same instance for chaining
             */
            public Mobile businessWebsite(String businessWebsite) {
                store.setBusinessWebsite(businessWebsite);
                return this;
            }

            /**
             * Sets the business' email
             *
             * @return the same instance for chaining
             */
            public Mobile businessEmail(String businessEmail) {
                store.setBusinessEmail(businessEmail);
                return this;
            }

            /**
             * Sets the business' category
             *
             * @return the same instance for chaining
             */
            public Mobile businessCategory(BusinessCategory businessCategory) {
                store.setBusinessCategory(businessCategory);
                return this;
            }

            /**
             * Expects the session to be already registered
             * This means that the verification code has already been sent to Whatsapp
             * If this is not the case, an exception will be thrown
             *
             * @return a non-null optional of whatsapp
             */
            public Optional<Whatsapp> registered() {
                if (!keys.registered()) {
                    return Optional.empty();
                }

                return Optional.of(Whatsapp.builder()
                        .customClient()
                        .store(store)
                        .keys(keys)
                        .errorHandler(errorHandler)
                        .build());
            }

            /**
             * Expects the session to still need registration
             * This means that you may or may not have a verification code, but that it hasn't already been sent to Whatsapp
             *
             * @return a non-null selector
             */
            public Whatsapp register(long phoneNumber, WhatsappVerificationHandler.Mobile verification) {
                Objects.requireNonNull(verification, "Expected a valid verification");
                if (!keys.registered()) {
                    var number = PhoneNumber.of(phoneNumber)
                            .orElseThrow(() -> new IllegalArgumentException(phoneNumber + " is not a valid phone number"));
                    keys.setPhoneNumber(number);
                    store.setPhoneNumber(number);
                    var registration = new MobileRegistration(
                            store,
                            keys,
                            verification
                    );
                    registration.registerPhoneNumber();
                    return Whatsapp.builder()
                            .customClient()
                            .store(store)
                            .keys(keys)
                            .errorHandler(errorHandler)
                            .build();
                }

                return Whatsapp.builder()
                        .customClient()
                        .store(store)
                        .keys(keys)
                        .errorHandler(errorHandler)
                        .build();
            }
        }
    }

    public static final class Custom extends WhatsappBuilder {
        private Store store;
        private Keys keys;
        private WhatsappErrorHandler errorHandler;
        private WhatsappVerificationHandler.Web webVerificationHandler;

        private Custom() {

        }

        public Custom store(Store store) {
            this.store = store;
            return this;
        }

        public Custom keys(Keys keys) {
            this.keys = keys;
            return this;
        }

        public Custom errorHandler(WhatsappErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Custom webVerificationSupport(WhatsappVerificationHandler.Web webVerificationHandler) {
            this.webVerificationHandler = webVerificationHandler;
            return this;
        }

        public Whatsapp build() {
            Objects.requireNonNull(store, "Expected a valid store");
            Objects.requireNonNull(keys, "Expected a valid keys");
            if (!Objects.equals(store.uuid(), keys.uuid())) {
                throw new IllegalArgumentException("UUID mismatch: %s != %s".formatted(store.uuid(), keys.uuid()));
            }
            return new Whatsapp(
                    store,
                    keys,
                    Objects.requireNonNullElse(errorHandler, WhatsappErrorHandler.toTerminal()),
                    getWebVerificationMethod(store, webVerificationHandler)
            );
        }

        private static WhatsappVerificationHandler.Web getWebVerificationMethod(Store store, WhatsappVerificationHandler.Web webVerificationHandler) {
            return switch (store.clientType()) {
                case WEB -> Objects.requireNonNullElse(webVerificationHandler, WhatsappVerificationHandler.Web.QrCode.toTerminal());
                case MOBILE -> null;
            };
        }
    }
}
