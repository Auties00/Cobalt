package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@UtilityClass
public class Jobs {
    private CompletableFuture<?> last;
    public CompletableFuture<Void> run(Runnable runnable){
        return run(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> run(Supplier<T> runnable){
        var future = CompletableFuture.supplyAsync(runnable);
        if(last == null){
            last = future;
            return future;
        }

        return last.thenComposeAsync(ignored -> future);
    }
}
