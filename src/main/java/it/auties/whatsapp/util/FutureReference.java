package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureReference<T> {
    @Nullable
    private T value;
    private CompletableFuture<T> future;

    @JsonCreator
    public FutureReference(@Nullable T initialValue) {
        this.value = Objects.requireNonNull(initialValue, "Missing value");
    }

    public FutureReference(@Nullable T initialValue, Supplier<CompletableFuture<T>> defaultValue) {
        this.value = initialValue;
        if(initialValue == null) {
            this.future = defaultValue.get();
        }
    }

    @JsonValue
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
