package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.info.MessageInfo;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * A model class that represents a pending reply that accepts only a result
 *
 * @param id     the non-null id of the message waiting for a reply
 * @param future the non-null completable result that will be resolved when a reply becomes available
 */
public record ReplyHandler(@NonNull String id, @NonNull CompletableFuture<MessageInfo> future) {
    /**
     * Constructs a new single reply
     *
     * @param id the non-null id of the message waiting for a reply
     * @return a non-null reply
     */
    public static ReplyHandler of(@NonNull String id) {
        return new ReplyHandler(id, new CompletableFuture<>());
    }
}

