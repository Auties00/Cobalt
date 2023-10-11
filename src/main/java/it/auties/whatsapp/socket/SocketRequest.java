package it.auties.whatsapp.socket;

import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.Specification;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@SuppressWarnings("UnusedReturnValue")
public record SocketRequest(String id, Object body, CompletableFuture<Node> future,
                            Function<Node, Boolean> filter, Throwable caller) {
    /**
     * The timeout in seconds before a Request wrapping a Node fails
     */
    private static final int TIMEOUT = 60;

    /**
     * The delayed executor used to cancel futures
     */
    private static final Executor EXECUTOR = delayedExecutor(TIMEOUT, SECONDS);

    private SocketRequest(String id, Function<Node, Boolean> filter, Object body) {
        this(id, body, new CompletableFuture<>(), filter, trace(body));
        EXECUTOR.execute(this::cancelTimedFuture);
    }

    private static Throwable trace(Object body) {
        var current = Exceptions.current(null);
        var actualStackTrace = Arrays.stream(current.getStackTrace())
                .filter(entry -> !entry.getClassName().equals(SocketRequest.class.getName()) && !entry.getClassName().equals(Node.class.getName()))
                .toArray(StackTraceElement[]::new);
        current.setStackTrace(actualStackTrace);
        return new RequestException(body instanceof Node node ? "%s node timed out".formatted(node.toString()) : "Binary timed out", current);
    }

    private void cancelTimedFuture() {
        if (future.isDone()) {
            return;
        }
        future.completeExceptionally(caller);
    }

    /**
     * Constructs a new request with the provided body expecting a newsletters
     */
    public static SocketRequest of(Node body, Function<Node, Boolean> filter) {
        return new SocketRequest(body.id(), filter, body);
    }

    /**
     * Constructs a new request with the provided body expecting a newsletters
     */
    public static SocketRequest of(byte[] body) {
        return new SocketRequest(null, null, body);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @param store   the store
     */
    public CompletableFuture<Node> sendWithPrologue(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, true, false);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store    the store
     * @param session  the WhatsappWeb's WebSocket session
     * @param prologue whether the prologue should be prepended to the request
     * @param response whether the request expects a newsletters
     * @return this request
     */
    public CompletableFuture<Node> send(SocketSession session, Keys keys, Store store, boolean prologue, boolean response) {
        var ciphered = encryptMessage(keys);
        var buffer = BytesHelper.newBuffer();
        buffer.writeBytes(prologue ? getPrologueData(store) : new byte[0]);
        buffer.writeInt(ciphered.length >> 16);
        buffer.writeShort(65535 & ciphered.length);
        buffer.writeBytes(ciphered);
        session.sendBinary(BytesHelper.readBuffer(buffer))
                .thenRunAsync(() -> onSendSuccess(store, response))
                .exceptionallyAsync(this::onSendError);
        return future;
    }

    private byte[] getPrologueData(Store store) {
        return switch (store.clientType()) {
            case WEB -> Specification.Whatsapp.WEB_PROLOGUE;
            case MOBILE -> Specification.Whatsapp.MOBILE_PROLOGUE;
        };
    }


    private byte[] encryptMessage(Keys keys) {
        var encodedBody = body();
        var body = getBody(encodedBody);
        return keys.writeKey()
                .map(bytes -> AesGcm.encrypt(keys.writeCounter(true), body, bytes))
                .orElse(body);
    }

    private byte[] getBody(Object encodedBody) {
        return switch (encodedBody) {
            case byte[] bytes -> bytes;
            case Node node -> {
                var encoder = new BinaryEncoder();
                yield encoder.encode(node);
            }
            case null, default ->
                    throw new IllegalArgumentException("Cannot create request, illegal body: %s".formatted(encodedBody));
        };
    }

    private void onSendSuccess(Store store, boolean response) {
        if (!response) {
            future.complete(null);
            return;
        }

        store.addRequest(this);
    }

    private Void onSendError(Throwable throwable) {
        future.completeExceptionally(new IOException("Cannot send %s, an unknown exception occurred".formatted(this), throwable));
        return null;
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Node> send(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, false, true);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Void> sendWithNoResponse(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, false, false)
                .thenRunAsync(() -> {
                });
    }

    /**
     * Completes this request using {@code newsletters}
     *
     * @param response the newsletters used to complete {@link SocketRequest#future}
     */
    public boolean complete(Node response, boolean exceptionally) {
        if (response == null) {
            future.complete(null);
            return true;
        }
        if (exceptionally) {
            future.completeExceptionally(new RuntimeException("Cannot process request %s with %s".formatted(this, response), caller));
            return true;
        }
        if (filter != null && !filter.apply(response)) {
            return false;
        }
        future.complete(response);
        return true;
    }
}
