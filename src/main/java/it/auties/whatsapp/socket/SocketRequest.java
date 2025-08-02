package it.auties.whatsapp.socket;

import it.auties.whatsapp.model.node.Node;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

public final class SocketRequest {
    private final String id;
    private final Object body;
    private final Function<Node, Boolean> filter;
    private volatile Node response;

    SocketRequest(String id, Function<Node, Boolean> filter, Object body) {
        this.id = id;
        this.body = body;
        this.filter = filter;
    }

    public boolean complete(Node response) {
        Objects.requireNonNull(response, "Response cannot be null");
        var acceptable = response == Node.empty()
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

    public String id() {
        return id;
    }

    public Object body() {
        return body;
    }

    public Node waitForResponse(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "Timeout cannot be null");
        if (response == null) {
            synchronized (this) {
                if(response == null) {
                    wait(timeout.toMillis());
                }
            }
        }
        return response;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || obj instanceof SocketRequest that && Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SocketRequest[" + "id=" + id + ']';
    }
}
