package it.auties.whatsapp.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class OrderedAsyncTaskRunner {
    private CompletableFuture<?> lastFuture;

    public <T> CompletableFuture<T> runAsync(Supplier<CompletableFuture<T>> future){
        var result = lastFuture == null ? future.get() : lastFuture.thenComposeAsync(ignored -> future.get());
        this.lastFuture = result;
        return result;
    }

    public void cancel(){
        if(lastFuture == null){
            return;
        }
        lastFuture.cancel(true);
    }
}
