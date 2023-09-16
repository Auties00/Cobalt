package it.auties.whatsapp.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureReference<T> {
    @Nullable
    private T value;
    private CompletableFuture<T> future;

    public FutureReference(@Nullable T initialValue, Supplier<CompletableFuture<T>> defaultValue) {
        this.value = initialValue;
        if(initialValue == null) {
            this.future = defaultValue.get();
        }
    }

    public T value() {
        if(future != null) {
            this.value = future.join();
            future = null;
        }

        return value;
    }

    public void setValue(@NonNull T value) {
        if(future != null) {
            future.cancel(true);
            future = null;
        }

        this.value = value;
    }
}
