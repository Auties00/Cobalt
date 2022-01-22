package it.auties.whatsapp.socket;

import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.WhatsappUtils;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp.crypto.Handshake.PROLOGUE;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@Log
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
        return new Request(WhatsappUtils.readNullableId(body), body, createTimedFuture());
    }

    private static CompletableFuture<Node> createTimedFuture() {
        return new CompletableFuture<Node>()
                .orTimeout(TIMEOUT, TimeUnit.SECONDS);
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
    public @NonNull CompletableFuture<Node> sendWithPrologue(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, true, false);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public @NonNull CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, false, true);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public @NonNull CompletableFuture<Node> sendWithNoResponse(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store) {
        return send(session, keys, store, false, false);
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
    public @NonNull CompletableFuture<Node> send(@NonNull Session session, @NonNull WhatsappKeys keys, @NonNull WhatsappStore store, boolean prologue, boolean response) {
        System.out.printf("Sending %s%n", body);
        var ciphered = cipherMessage(keys);
        var buffer = Buffers.newBuffer(prologue ? PROLOGUE : new byte[0])
                .writeInt(ciphered.length >> 16)
                .writeShort(65535 & ciphered.length)
                .writeBytes(ciphered);
        session.getAsyncRemote().sendBinary(Buffers.readBinary(buffer).toBuffer(),
                result -> handleSendResult(store, result, response));
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
        var body = switch (body()) {
            case byte[] bytes -> bytes;
            case Node node -> ENCODER.encode(node);
            default -> throw new IllegalStateException("Illegal body: " + body());
        };

        if (keys.writeKey() == null) {
            return body;
        }

        return AesGmc.with(keys.writeKey(), keys.writeCounter(true), true)
                .process(body);
    }
}
