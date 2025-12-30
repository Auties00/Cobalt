package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.exception.NodeTimeoutException;
import com.github.auties00.cobalt.node.Node;

import java.time.Duration;
import java.util.function.Function;

public final class SocketRequest {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final Node body;
    private final Function<Node, Boolean> filter;
    private volatile Node response;

    public SocketRequest(Node body, Function<Node, Boolean> filter) {
        this.body = body;
        this.filter = filter;
    }

    public boolean complete(Node response) {
        var acceptable = response == null
                || filter == null
                || filter.apply(response);
        if (acceptable) {
            synchronized (this) {
                this.response = response;
                notifyAll();
            }
        }
        return acceptable;
    }

    public Node waitForResponse() {
        if (response == null) {
            synchronized (this) {
                if (response == null) {
                    try {
                        wait(TIMEOUT.toMillis());
                        if (response == null) {
                            throw new NodeTimeoutException(body);
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new NodeTimeoutException(body);
                    }
                }
            }
        }
        return response;
    }
}
