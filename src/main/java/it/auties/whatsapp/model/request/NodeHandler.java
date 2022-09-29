package it.auties.whatsapp.model.request;

import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public record NodeHandler(@NonNull Predicate<Node> predicate, @NonNull CompletableFuture<Node> future) {
    public static NodeHandler of(@NonNull Predicate<Node> predicate){
        return new NodeHandler(predicate, new CompletableFuture<>());
    }
}
