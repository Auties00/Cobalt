package it.auties.whatsapp.api;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * An interface to represent a supplier that returns a code wrapped in a CompletableFuture
 */
public interface AsyncCaptchaCodeSupplier extends Function<VerificationCodeResponse, CompletableFuture<String>> {
    /**
     * Creates an asynchronous supplier from a synchronous one
     *
     * @param supplier a non-null supplier
     * @return a non-null async supplier
     */
    static AsyncCaptchaCodeSupplier of(Function<VerificationCodeResponse, String> supplier) {
        return (response) -> CompletableFuture.completedFuture(supplier.apply(response));
    }
}
