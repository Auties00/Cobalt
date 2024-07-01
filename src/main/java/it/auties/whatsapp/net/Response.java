package it.auties.whatsapp.net;

import java.util.concurrent.CompletableFuture;

public sealed interface Response<V> {
    default boolean complete(V result) {
        switch (this) {
            case Callback<V> callback -> {
                try {
                    callback.onResult(result, null);
                }catch (Throwable throwable) {
                    // Do not propagate
                }
            }
            case Future<V> future -> future.complete(result);
        }

        return true;
    }

    @SuppressWarnings("UnusedReturnValue") // Would violate the return value of CompletableFuture
    default boolean completeExceptionally(Throwable throwable) {
        switch (this) {
            case Callback<V> callback -> callback.onResult(null, throwable);
            case Future<V> future -> future.completeExceptionally(throwable);
        }

        return true;
    }


    final class Future<V> extends CompletableFuture<V> implements Response<V> {

    }

    @FunctionalInterface
    non-sealed interface Callback<V> extends Response<V> {
        void onResult(V result, Throwable error);
    }
}
