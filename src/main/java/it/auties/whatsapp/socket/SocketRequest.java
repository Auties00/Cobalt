package it.auties.whatsapp.socket;

import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.io.BinaryEncoder;
import it.auties.whatsapp.model.node.Node;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

public record SocketRequest(String id, Object body, CompletableFuture<Node> future, Function<Node, Boolean> filter) {
    private static final int TIMEOUT = 60;

    private SocketRequest(String id, Function<Node, Boolean> filter, Object body) {
        this(id, body, futureOrTimeout(body), filter);
    }

    private static CompletableFuture<Node> futureOrTimeout(Object body) {
        return new CompletableFuture<Node>().orTimeout(TIMEOUT, SECONDS).exceptionally(throwable -> {
            throw new RequestException("Node timed out: " + body);
        });
    }

    static SocketRequest of(Node body, Function<Node, Boolean> filter) {
        return new SocketRequest(body.id(), filter, body);
    }

    static SocketRequest of(byte[] body) {
        return new SocketRequest(null, null, body);
    }

    byte[] toBytes() {
        return switch (body) {
            case byte[] bytes -> bytes;
            case Node node -> {
                try(var encoder = new BinaryEncoder()) {
                    yield encoder.encode(node);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
            case null, default -> throw new IllegalArgumentException("Cannot create request, illegal body: %s".formatted(body));
        };
    }

    public boolean complete(Node response, boolean exceptionally) {
        if (response == null) {
            future.complete(Node.of("error", Map.of("closed", true))); // Prevent NPEs all over the place
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
}
