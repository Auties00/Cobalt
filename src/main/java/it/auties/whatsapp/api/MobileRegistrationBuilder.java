package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.util.RegistrationHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder to specify the options for the mobile api
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public sealed class MobileRegistrationBuilder<T extends MobileRegistrationBuilder<T>> {
    protected Whatsapp whatsapp;
    protected final Store store;
    protected final Keys keys;
    protected final ErrorHandler errorHandler;
    protected final Executor socketExecutor;
    protected AsyncVerificationCodeSupplier verificationCodeSupplier;
    protected AsyncCaptchaCodeSupplier verificationCaptchaSupplier;

    /**
     * Sets the handler that provides the verification code when verifying an account
     *
     * @param verificationCodeSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCodeSupplier(@NonNull Supplier<String> verificationCodeSupplier) {
        this.verificationCodeSupplier = AsyncVerificationCodeSupplier.of(verificationCodeSupplier);
        return (T) this;
    }

    /**
     * Sets the handler that provides the captcha result when verifying an account
     * Happens only on business devices
     *
     * @param verificationCaptchaSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCaptchaSupplier(@NonNull Function<VerificationCodeResponse, String> verificationCaptchaSupplier){
        this.verificationCaptchaSupplier = AsyncCaptchaCodeSupplier.of(verificationCaptchaSupplier);
        return (T) this;
    }

    /**
     * Sets the handler that provides the verification code when verifying an account
     *
     * @param verificationCodeSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCodeSupplier(@NonNull AsyncVerificationCodeSupplier verificationCodeSupplier){
        this.verificationCodeSupplier = verificationCodeSupplier;
        return (T) this;
    }

    /**
     * Sets the handler that provides the captcha result when verifying an account
     * Happens only on business devices
     *
     * @param verificationCaptchaSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCaptchaSupplier(@NonNull AsyncCaptchaCodeSupplier verificationCaptchaSupplier){
        this.verificationCaptchaSupplier = verificationCaptchaSupplier;
        return (T) this;
    }

    protected Whatsapp buildWhatsapp() {
        return this.whatsapp = Whatsapp.customBuilder()
                .store(store)
                .keys(keys)
                .errorHandler(errorHandler)
                .socketExecutor(socketExecutor)
                .build();
    }

    public final static class Unregistered extends MobileRegistrationBuilder<Unregistered> {
        private VerificationCodeMethod verificationCodeMethod;

        public Unregistered(Store store, Keys keys, ErrorHandler errorHandler, Executor socketExecutor) {
            super(store, keys, errorHandler, socketExecutor);
            this.verificationCodeMethod = VerificationCodeMethod.SMS;
        }


        /**
         * Sets the type of method used to verify the account
         *
         * @param verificationCodeMethod the non-null method
         * @return the same instance
         */
        public Unregistered verificationCodeMethod(@NonNull VerificationCodeMethod verificationCodeMethod){
            this.verificationCodeMethod = verificationCodeMethod;
            return this;
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber) {
            if(whatsapp != null) {
                return CompletableFuture.completedFuture(whatsapp);
            }

            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            Objects.requireNonNull(verificationCodeMethod, "Expected a valid verification method");
            var number = PhoneNumber.of(phoneNumber);
            keys.phoneNumber(number);
            store.phoneNumber(number);
            if (!keys.registered()) {
                return RegistrationHelper.registerPhoneNumber(store, keys, verificationCodeSupplier, verificationCaptchaSupplier, verificationCodeMethod)
                        .thenApply(ignored -> buildWhatsapp());
            }

            return CompletableFuture.completedFuture(buildWhatsapp());
        }

        /**
         * Asks Whatsapp for a one-time-password to start the registration process
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<Unverified> requestVerificationCode(long phoneNumber) {
            var number = PhoneNumber.of(phoneNumber);
            keys.phoneNumber(number);
            store.phoneNumber(number);
            if (!keys.registered()) {
                return RegistrationHelper.requestVerificationCode(store, keys, verificationCodeMethod)
                        .thenApply(ignored -> new Unverified(store, keys, errorHandler, socketExecutor));
            }

            return CompletableFuture.completedFuture(new Unverified(store, keys, errorHandler, socketExecutor));
        }
    }

    public final static class Unverified extends MobileRegistrationBuilder<Unverified> {


        public Unverified(Store store, Keys keys, ErrorHandler errorHandler, Executor socketExecutor) {
            super(store, keys, errorHandler, socketExecutor);
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(long phoneNumber) {
            var number = PhoneNumber.of(phoneNumber);
            keys.phoneNumber(number);
            store.phoneNumber(number);
            return verify();
        }


        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify() {
            Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            return RegistrationHelper.sendVerificationCode(store, keys, verificationCodeSupplier, verificationCaptchaSupplier)
                    .thenApply(ignored -> buildWhatsapp());
        }
    }
}
