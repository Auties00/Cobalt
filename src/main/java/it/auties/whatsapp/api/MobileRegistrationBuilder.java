package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.util.MobileRegistration;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A builder to specify the options for the mobile api
 */
@SuppressWarnings("unused")
public sealed class MobileRegistrationBuilder {
    final Store store;
    final Keys keys;
    final ErrorHandler errorHandler;
    RegisteredResult result;
    boolean printRequests;
    AsyncVerificationCodeSupplier verificationCodeSupplier;

    MobileRegistrationBuilder(Store store, Keys keys, ErrorHandler errorHandler) {
        this.store = store;
        this.keys = keys;
        this.errorHandler = errorHandler;
        this.printRequests = false;
    }

    public final static class Unregistered extends MobileRegistrationBuilder {
        private UnverifiedResult unregisteredResult;
        private VerificationCodeMethod verificationCodeMethod;
        private boolean autocloseCloudVerificationClient;

        Unregistered(Store store, Keys keys, ErrorHandler errorHandler) {
            super(store, keys, errorHandler);
            this.verificationCodeMethod = VerificationCodeMethod.SMS;
        }

        public Unregistered verificationCodeSupplier(Supplier<String> verificationCodeSupplier) {
            this.verificationCodeSupplier = AsyncVerificationCodeSupplier.of(verificationCodeSupplier);
            return this;
        }

        public Unregistered verificationCodeSupplier(AsyncVerificationCodeSupplier verificationCodeSupplier) {
            this.verificationCodeSupplier = verificationCodeSupplier;
            return this;
        }

        public Unregistered device(CompanionDevice device) {
            store.setDevice(device);
            return this;
        }

        public Unregistered proxy(URI proxy) {
            store.setProxy(proxy);
            return this;
        }

        public Unregistered verificationCodeMethod(VerificationCodeMethod verificationCodeMethod) {
            this.verificationCodeMethod = verificationCodeMethod;
            return this;
        }

        public Unregistered printRequests(boolean printRequests) {
            this.printRequests = printRequests;
            return this;
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<RegisteredResult> register(long phoneNumber) {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }

            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            Objects.requireNonNull(verificationCodeMethod, "Expected a valid verification method");
            if (!keys.registered()) {
                var number = PhoneNumber.of(phoneNumber);
                keys.setPhoneNumber(number);
                store.setPhoneNumber(number);
                var registration = new MobileRegistration(
                        store,
                        keys,
                        verificationCodeSupplier,
                        verificationCodeMethod
                );
                return registration.registerPhoneNumber()
                        .thenApplyAsync(response -> {
                            var api = Whatsapp.customBuilder()
                                    .store(store)
                                    .keys(keys)
                                    .errorHandler(errorHandler)
                                    .build();
                            return this.result = new RegisteredResult(api, Optional.ofNullable(response));
                        });
            }

            var api = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .build();
            return CompletableFuture.completedFuture(result);
        }

        /**
         * Asks Whatsapp for a one-time-password to start the registration process
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<UnverifiedResult> requestVerificationCode(long phoneNumber) {
            if(unregisteredResult != null) {
                return CompletableFuture.completedFuture(unregisteredResult);
            }

            var number = PhoneNumber.of(phoneNumber);
            keys.setPhoneNumber(number);
            store.setPhoneNumber(number);
            if (!keys.registered()) {
                var registration = new MobileRegistration(
                        store,
                        keys,
                        verificationCodeSupplier,
                        verificationCodeMethod
                );
                return registration.requestVerificationCode().thenApply(response -> {
                    var unverified = new Unverified(store, keys, errorHandler, verificationCodeSupplier);
                    return this.unregisteredResult = new UnverifiedResult(unverified, Optional.ofNullable(response));
                });
            }

            var unverified = new Unverified(store, keys, errorHandler, verificationCodeSupplier);
            return CompletableFuture.completedFuture(this.unregisteredResult = new UnverifiedResult(unverified, Optional.empty()));
        }
    }

    public final static class Unverified extends MobileRegistrationBuilder {
        Unverified(Store store, Keys keys, ErrorHandler errorHandler, AsyncVerificationCodeSupplier verificationCodeSupplier) {
            super(store, keys, errorHandler);
            this.verificationCodeSupplier = verificationCodeSupplier;
        }

        public Unverified verificationCodeSupplier(Supplier<String> verificationCodeSupplier) {
            this.verificationCodeSupplier = AsyncVerificationCodeSupplier.of(verificationCodeSupplier);
            return this;
        }

        public Unverified verificationCodeSupplier(AsyncVerificationCodeSupplier verificationCodeSupplier) {
            this.verificationCodeSupplier = verificationCodeSupplier;
            return this;
        }

        public Unverified device(CompanionDevice device) {
            store.setDevice(device);
            return this;
        }

        public Unverified proxy(URI proxy) {
            store.setProxy(proxy);
            return this;
        }

        public Unverified printRequests(boolean printRequests) {
            this.printRequests = printRequests;
            return this;
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<RegisteredResult> verify(long phoneNumber) {
            var number = PhoneNumber.of(phoneNumber);
            keys.setPhoneNumber(number);
            store.setPhoneNumber(number);
            return verify();
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<RegisteredResult> verify() {
            if(result != null) {
                return CompletableFuture.completedFuture(result);
            }

            Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            var registration = new MobileRegistration(
                    store,
                    keys,
                    verificationCodeSupplier,
                    VerificationCodeMethod.NONE
            );
            return registration.sendVerificationCode().thenApply(response -> {
                var api = Whatsapp.customBuilder()
                        .store(store)
                        .keys(keys)
                        .errorHandler(errorHandler)
                        .build();
                return this.result = new RegisteredResult(api, Optional.ofNullable(response));
            });
        }
    }

    public record RegisteredResult(Whatsapp whatsapp, Optional<RegistrationResponse> response) {

    }

    public record UnverifiedResult(Unverified unverified, Optional<RegistrationResponse> response) {

    }
}
