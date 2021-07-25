package it.auties.whatsapp4j.utils.internal;

import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for {@link CompletableFuture}
 */
@UtilityClass
public class FutureUtils {
    public void handleExceptions(CompletableFuture<?> future){
        future.exceptionallyAsync(exception -> {
            exception.printStackTrace();
            return null;
        });
    }
}
