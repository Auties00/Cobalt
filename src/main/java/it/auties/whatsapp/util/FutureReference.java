package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureReference<T> {
    private T value;
    private CompletableFuture<T> future;

    @JsonCreator
    public FutureReference(T initialValue) {
        this.value = Objects.requireNonNull(initialValue, "Missing value");
    }

    public FutureReference(T initialValue, Supplier<CompletableFuture<T>> defaultValue) {
        this.value = initialValue;
        if (initialValue == null) {
            this.future = defaultValue.get();
        }
    }

    @JsonValue
    public T value() {
        if (future != null) {
            this.value = future.join();
            future = null;
        }

        return value;
    }

    public void setValue(T value) {
        if (future != null) {
            future.cancel(true);
            future = null;
        }

        this.value = value;
    }
}
