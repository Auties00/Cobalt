package it.auties.whatsapp.api;

import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An interface to represent a supplier that returns a code wrapped in a CompletableFuture
 */
public interface AsyncVerificationCodeSupplier extends Supplier<CompletableFuture<String>> {
    /**
     * Creates an asynchronous supplier from a syncronous one
     *
     * @param supplier a non-null supplier
     * @return a non-null async supplier
     */
    static AsyncVerificationCodeSupplier of(@NonNull Supplier<String> supplier){
        return () -> CompletableFuture.completedFuture(supplier.get());
    }
}
