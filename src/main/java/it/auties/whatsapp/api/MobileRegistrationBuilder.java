package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.RegistrationStatus;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.util.RegistrationHelper;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A builder to specify the options for the mobile api
 */
@AllArgsConstructor
@SuppressWarnings("unused")
public sealed class MobileRegistrationBuilder {
    protected final Store store;
    protected final Keys keys;

    public final static class Unregistered extends MobileRegistrationBuilder {
        Unregistered(Store store, Keys keys) {
            super(store, keys);
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @param handler the non-null method to use to get the verification code
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber, @NonNull Supplier<String> handler) {
            return register(phoneNumber, AsyncVerificationCodeSupplier.of(handler));
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @param handler the non-null method to use to get the verification code
         * @param method the non-null verification method to use
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber, @NonNull VerificationCodeMethod method, @NonNull Supplier<String> handler) {
            return register(phoneNumber, method, AsyncVerificationCodeSupplier.of(handler));
        }


        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @param handler the non-null method to use to get the verification code
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber, @NonNull AsyncVerificationCodeSupplier handler) {
            return register(phoneNumber, VerificationCodeMethod.SMS, handler);
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @param handler the non-null method to use to get the verification code
         * @param method the non-null verification method to use
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber, @NonNull VerificationCodeMethod method, @NonNull AsyncVerificationCodeSupplier handler) {
            return keys.registrationStatus() == RegistrationStatus.UNREGISTERED ? RegistrationHelper.registerPhoneNumber(store.phoneNumber(PhoneNumber.of(phoneNumber)), keys, handler, method)
                    .thenApply(ignored -> new Whatsapp(store, keys)) : CompletableFuture.completedFuture(new Whatsapp(store, keys));
        }

        /**
         * Asks Whatsapp for a one-time-password to start the registration process
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<Unverified> requestVerificationCode(long phoneNumber) {
            return requestVerificationCode(phoneNumber, VerificationCodeMethod.SMS);
        }

        /**
         * Asks Whatsapp for a one-time-password to start the registration process
         *
         * @param phoneNumber a phone number(include the prefix)
         * @param method the non-null verification method to use
         * @return a future
         */
        public CompletableFuture<Unverified> requestVerificationCode(long phoneNumber, @NonNull VerificationCodeMethod method) {
            return keys.registrationStatus() == RegistrationStatus.UNREGISTERED ? RegistrationHelper.requestVerificationCode(store.phoneNumber(PhoneNumber.of(phoneNumber)), keys, method)
                    .thenApply(ignored -> new Unverified(store, keys)) : CompletableFuture.completedFuture(new Unverified(store, keys));
        }
    }

    public final static class Unverified extends MobileRegistrationBuilder {
        Unverified(Store store, Keys keys) {
            super(store, keys);
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(long phoneNumber, Supplier<String> handler) {
            store.phoneNumber(PhoneNumber.of(phoneNumber));
            return verify(handler);
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(Supplier<String> handler) {
            return verify(AsyncVerificationCodeSupplier.of(handler));
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(long phoneNumber, AsyncVerificationCodeSupplier handler) {
            store.phoneNumber(PhoneNumber.of(phoneNumber));
            return verify(handler);
        }


        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(AsyncVerificationCodeSupplier handler) {
            Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
            return RegistrationHelper.sendVerificationCode(store, keys, handler)
                    .thenApply(ignored -> new Whatsapp(store, keys));
        }
    }
}
