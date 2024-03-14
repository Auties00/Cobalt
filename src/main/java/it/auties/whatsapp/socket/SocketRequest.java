package it.auties.whatsapp.socket;

import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Specification;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

public record SocketRequest(String id, Object body, CompletableFuture<Node> future,
                            Function<Node, Boolean> filter) {
    private static final int TIMEOUT = 60;

    private SocketRequest(String id, Function<Node, Boolean> filter, Object body) {
        this(id, body, futureOrTimeout(), filter);
    }

    private static CompletableFuture<Node> futureOrTimeout() {
        return new CompletableFuture<Node>().orTimeout(TIMEOUT, SECONDS);
    }

    public static SocketRequest of(Node body, Function<Node, Boolean> filter) {
        return new SocketRequest(body.id(), filter, body);
    }

    public static SocketRequest of(byte[] body) {
        return new SocketRequest(null, null, body);
    }

    public CompletableFuture<Node> sendWithPrologue(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, true, false);
    }

    public CompletableFuture<Node> send(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, false, true);
    }

    public CompletableFuture<Node> send(SocketSession session, Keys keys, Store store, boolean prologue, boolean response) {
        var ciphered = encryptMessage(keys);
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try(var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            if(prologue) {
                dataOutputStream.write(getPrologueData(store));
            }
            dataOutputStream.writeInt(ciphered.length >> 16);
            dataOutputStream.writeShort(65535 & ciphered.length);
            dataOutputStream.write(ciphered);
            session.sendBinary(byteArrayOutputStream.toByteArray())
                    .thenRunAsync(() -> onSendSuccess(store, response))
                    .exceptionallyAsync(error -> onSendError());
            return future;
        }catch (IOException exception) {
            throw new RequestException(exception);
        }
    }

    public CompletableFuture<Void> sendWithNoResponse(SocketSession session, Keys keys, Store store) {
        return send(session, keys, store, false, false)
                .thenRun(() -> {});
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
                try(var encoder = new BinaryEncoder()) {
                    yield encoder.encode(node);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
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

    public boolean complete(Node response, boolean exceptionally) {
        if (response == null) {
            onSendError();
            return true;
        }

        if (exceptionally) {
            future.completeExceptionally(new RuntimeException("Cannot process request %s with %s".formatted(this, response)));
            return true;
        }

        if (filter != null && !filter.apply(response)) {
            return false;
        }

        future.complete(response);
        return true;
    }

    private <T> T onSendError() {
        future.complete(Node.of("error", Map.of("closed", true))); // Prevent NPEs all over the place
        return null;
    }
}
