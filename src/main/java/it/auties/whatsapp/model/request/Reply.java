package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.request.Reply.Single;
import it.auties.whatsapp.model.request.Reply.Multi;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A model class that represents a pending reply
 *
 */
public sealed interface Reply permits Single, Multi {
    /**
     * The non-null id of the message waiting for a reply
     *
     * @return a non-null string
     */
    String id();

    /**
     * A model class that represents a pending reply that accepts only a result
     *
     * @param id the non-null id of the message waiting for a reply
     * @param future the non-null completable future that will be resolved when a reply becomes available
     */
    record Single(@NonNull String id, @NonNull CompletableFuture<MessageInfo> future) implements Reply {
        /**
         * Constructs a new single reply
         *
         * @param id the non-null id of the message waiting for a reply
         * @return a non-null reply
         */
        public static Single of(@NonNull String id){
            return new Single(id, new CompletableFuture<>());
        }
    }

    /**
     * A model class that represents a pending reply that accepts any amount of responses
     *
     * @param id the non-null id of the message waiting for a reply
     * @param onReply the non-null reply consumer
     */
    record Multi(@NonNull String id, @NonNull Consumer<MessageInfo> onReply) implements Reply {
        /**
         * Constructs a new multi reply
         *
         * @param id the non-null id of the message waiting for a reply
         * @param onReply the non-null reply consumer
         * @return a non-null reply
         */
        public static Multi of(@NonNull String id, @NonNull Consumer<MessageInfo> onReply){
            return new Multi(id, onReply);
        }
    }
}
