package it.auties.whatsapp.cipher;

import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.utils.WhatsappUtils;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@ToString(exclude = "future")
@Log
public class Request {
    /**
     * The binary encoder, used to encode requests that take as a parameter a node
     */
    private static final BinaryEncoder NODE_ENCODER = new BinaryEncoder();

    /**
     * The id of this request
     */
    @Getter
    private final String id;

    /**
     * The body of the request
     */
    private final @NonNull Object body;

    /**
     * A future completed when Whatsapp sends a response or immediately after this request is sent if no response is expected
     */
    private final @NonNull CompletableFuture<Node> future = new CompletableFuture<>();

    /**
     * Constructs a new request with the provided body expecting a response
     */
    public static Request with(@NonNull Node body){
        System.out.printf("Sending: %s%n", body);
        return new Request(WhatsappUtils.readNullableId(body), NODE_ENCODER.encode(body));
    }

    /**
     * Constructs a new request with the provided body expecting a response
     */
    public static Request with(@NonNull Object body){
        System.out.println("Sending a protobuf");
        return new Request(null, ProtobufEncoder.encode(body));
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store    the store
     * @param session  the WhatsappWeb's WebSocket session
     * @return this request
     */
    public @NonNull CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store){
        return send(session, keys, store, false);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store    the store
     * @param session  the WhatsappWeb's WebSocket session
     * @param prologue whether the prologue should be prepended to the request
     * @return this request
     */
    public @NonNull CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store, boolean prologue){
        var encryptedBody = Cipher.cipherMessage(parseBodyOrThrow(), keys.writeKey(), store.writeCounter().getAndIncrement(), prologue);
        session.getAsyncRemote()
                .sendBinary(encryptedBody.toBuffer(), result -> handleSendResult(store, result));
        return future;
    }

    /**
     * Completes this request using {@code response}
     *
     * @param response the response used to complete {@link Request#future}
     */
    public void complete(@NonNull Node response, boolean exceptionally){
        if(exceptionally){
            log.warning("Whatsapp could not process %s: %s".formatted(this, response));
            future.completeExceptionally(new RuntimeException("Whatsapp could not process %s: %s".formatted(this, response)));
            return;
        }

        future.complete(response);
    }

    private byte[] parseBodyOrThrow() {
        return switch (body){
            case byte[] bytes -> bytes;
            case Node node -> NODE_ENCODER.encode(node);
            default -> throw new IllegalStateException("Illegal body: " + body);
        };
    }

    private void handleSendResult(WhatsappStore store, SendResult result) {
        if (!result.isOK()) {
            future.completeExceptionally(new IllegalArgumentException("Cannot send %s".formatted(this), result.getException()));
            return;
        }

        store.pendingRequests().add(this);
    }
}
