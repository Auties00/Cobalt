package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.crypto.AesGmc;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static it.auties.whatsapp.crypto.Handshake.PROLOGUE;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@Log
@SuppressWarnings("UnusedReturnValue")
public record Request(String id, @NonNull Object body, @NonNull CompletableFuture<Node> future) {
    /**
     * The binary encoder, used to encode requests that take as a parameter a node
     */
    private static final BinaryEncoder ENCODER = new BinaryEncoder();

    /**
     * The timeout in seconds before a Request wrapping a Node fails
     */
    private static final int TIMEOUT = 10;

    /**
     * Constructs a new request with the provided body expecting a response
     */
    public static Request with(@NonNull Node body) {
        var future = new CompletableFuture<Node>();
        delayedExecutor(TIMEOUT, SECONDS)
                .execute(() -> cancelTimedFuture(future, body));
        return new Request(body.id(), body, future);
    }

    private static void cancelTimedFuture(CompletableFuture<Node> future, Node node) {
        if(future.isDone()){
            return;
        }

        future.completeExceptionally(new TimeoutException("%s timed out: no response from WhatsApp".formatted(node)));
    }

    /**
     * Constructs a new request with the provided body expecting a response
     */
    public static Request with(@NonNull Object body) {
        return new Request(null, ProtobufEncoder.encode(body), new CompletableFuture<>());
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @param store   the store
     */
    public CompletableFuture<Node> sendWithPrologue(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, true, false);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, false, true);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Void> sendWithNoResponse(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, false, false)
                .thenRunAsync(() -> {});
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store    the store
     * @param session  the WhatsappWeb's WebSocket session
     * @param prologue whether the prologue should be prepended to the request
     * @param response whether the request expects a response
     * @return this request
     */
    public CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store, boolean prologue, boolean response) {
        try {
            var ciphered = cipherMessage(keys);
            var buffer = Bytes.of(prologue ? PROLOGUE : new byte[0])
                    .appendInt(ciphered.length >> 16)
                    .appendShort(65535 & ciphered.length)
                    .append(ciphered)
                    .toNioBuffer();
            session.getAsyncRemote()
                    .sendBinary(buffer, result -> handleSendResult(store, result, response));
        }catch (Exception exception){
            future.completeExceptionally(new RuntimeException("Cannot send request", exception));
        }

        return future;
    }

    /**
     * Completes this request using {@code response}
     *
     * @param response the response used to complete {@link Request#future}
     */
    public void complete(@NonNull Node response, boolean exceptionally) {
        if (exceptionally) {
            log.warning("Whatsapp could not process %s: %s".formatted(this, response));
            future.completeExceptionally(new RuntimeException("Whatsapp could not process %s: %s".formatted(this, response)));
            return;
        }

        future.complete(response);
    }

    private void handleSendResult(WhatsappStore store, SendResult result, boolean response) {
        if (!result.isOK()) {
            log.warning("Whatsapp could not send %s: %s".formatted(this, response));
            future.completeExceptionally(new IllegalArgumentException("Cannot send %s".formatted(this), result.getException()));
            return;
        }

        if(!response){
            future.complete(null);
            return;
        }

        store.pendingRequests().add(this);
    }

    private byte[] cipherMessage(WhatsappKeys keys) {
        var encodedBody = body();
        var body = switch (encodedBody) {
            case byte[] bytes -> bytes;
            case Node node -> ENCODER.encode(node);
            default -> throw new IllegalStateException("Cannot create request, illegal body: %s".formatted(encodedBody));
        };

        if (keys.writeKey() == null) {
            return body;
        }

        return AesGmc.with(keys.writeKey(), keys.writeCounter(true), true)
                .process(body);
    }
}