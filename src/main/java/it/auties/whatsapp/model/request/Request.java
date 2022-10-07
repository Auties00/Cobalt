package it.auties.whatsapp.model.request;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Encoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.exception.ErroneousBinaryRequest;
import it.auties.whatsapp.exception.ErroneousNodeException;
import it.auties.whatsapp.exception.Exceptions;
import it.auties.whatsapp.util.JacksonProvider;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static it.auties.whatsapp.crypto.Handshake.PROLOGUE;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An abstract model class that represents a request made from the client to the server.
 */
@SuppressWarnings("UnusedReturnValue")
public record Request(String id, @NonNull Object body, @NonNull CompletableFuture<Node> future, Throwable caller)
        implements JacksonProvider {
    /**
     * The binary encoder, used to encode requests that take as a parameter a node
     */
    private static final Encoder ENCODER = new Encoder();

    /**
     * The timeout in seconds before a Request wrapping a Node fails
     */
    private static final int TIMEOUT = 60;

    private Request(String id, @NonNull Object body) {
        this(id, body, new CompletableFuture<>(), Exceptions.current());
        delayedExecutor(TIMEOUT, SECONDS).execute(this::cancelTimedFuture);
    }

    /**
     * Constructs a new request with the provided body expecting a response
     */
    public static Request of(@NonNull Node body) {
        return new Request(body.id(), body);
    }

    /**
     * Constructs a new request with the provided body expecting a response
     */
    @SneakyThrows
    public static Request of(@NonNull Object body) {
        return new Request(null, PROTOBUF.writeValueAsBytes(body));
    }

    private void cancelTimedFuture() {
        if (future.isDone()) {
            return;
        }

        var exception = body instanceof Node node ? new ErroneousNodeException("Node timed out(%s), no response from WhatsApp".formatted(node), node, caller)
                : new ErroneousBinaryRequest("Binary timed out, no response from WhatsApp", body, caller);
        future.completeExceptionally(exception);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @param store   the store
     */
    public CompletableFuture<Node> sendWithPrologue(@NonNull Session session, @NonNull Keys keys,
                                                    @NonNull Store store) {
        return send(session, keys, store, true, false);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Node> send(@NonNull Session session, @NonNull Keys keys, @NonNull Store store) {
        return send(session, keys, store, false, true);
    }

    /**
     * Sends a request to the WebSocket linked to {@code session}.
     *
     * @param store   the store
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    public CompletableFuture<Void> sendWithNoResponse(@NonNull Session session, @NonNull Keys keys,
                                                      @NonNull Store store) {
        return send(session, keys, store, false, false).thenRunAsync(() -> {
        });
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
    public CompletableFuture<Node> send(@NonNull Session session, @NonNull Keys keys, @NonNull Store store,
                                        boolean prologue, boolean response) {
        try {
            var ciphered = encryptMessage(keys);
            var buffer = Bytes.of(prologue ?
                            PROLOGUE :
                            new byte[0])
                    .appendInt(ciphered.length >> 16)
                    .appendShort(65535 & ciphered.length)
                    .append(ciphered)
                    .toNioBuffer();
            session.getAsyncRemote()
                    .sendBinary(buffer, result -> handleSendResult(store, result, response));
        } catch (Exception exception) {
            future.completeExceptionally(new IOException("Cannot send %s, an unknown exception occured".formatted(this), exception));
        }

        return future;
    }

    /**
     * Completes this request using {@code response}
     *
     * @param response the response used to complete {@link Request#future}
     */
    public void complete(Node response, boolean exceptionally) {
        if (response == null) {
            future.complete(Node.of("xmlstreamend"));
            return;
        }

        if (exceptionally || isErroneousNode(response)) {
            future.completeExceptionally(
                    new ErroneousNodeException("Cannot process request %s with %s".formatted(this, response), response,
                            caller));
            return;
        }

        future.complete(response);
    }

    private boolean isErroneousNode(Node response) {
        return response.attributes()
                .getOptionalString("type")
                .filter("error"::equals)
                .isPresent();
    }

    private void handleSendResult(Store store, SendResult result, boolean response) {
        if (!result.isOK()) {
            future.completeExceptionally(
                    new IOException("Cannot send request %s, erroneous send result: %s".formatted(this, result),
                            result.getException()));
            return;
        }

        if (!response) {
            future.complete(null);
            return;
        }

        store.addPendingRequest(this);
    }

    private byte[] encryptMessage(Keys keys) {
        var encodedBody = body();
        var body = switch (encodedBody) {
            case byte[] bytes -> bytes;
            case Node node -> ENCODER.encode(node);
            default -> throw new IllegalArgumentException(
                    "Cannot create request, illegal body: %s".formatted(encodedBody));
        };

        if (keys.writeKey() == null) {
            return body;
        }

        return AesGmc.cipher(keys.writeCounter(true), body, keys.writeKey().toByteArray(), true);
    }
}
