package it.auties.whatsapp.socket;

import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.model.node.Node;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

public record SocketRequest(String id, Object body, CompletableFuture<Node> future, Function<Node, Boolean> filter) {
    private static final int TIMEOUT = 60;

    SocketRequest(String id, Function<Node, Boolean> filter, Object body) {
        this(id, body, futureOrTimeout(body), filter);
    }

    private static CompletableFuture<Node> futureOrTimeout(Object body) {
        return new CompletableFuture<Node>()
                .orTimeout(TIMEOUT, SECONDS)
                .exceptionally(throwable -> {
                    throw new RequestException("Node timed out: " + body);
                });
    }

    public boolean complete(Node response) {
        var acceptable = response == Node.empty()
                || filter == null
                || filter.apply(response);
        if(acceptable) {
            future.complete(response);
        }
        return acceptable;
    }
}
