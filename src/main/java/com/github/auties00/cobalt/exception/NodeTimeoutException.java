package com.github.auties00.cobalt.exception;

import com.github.auties00.cobalt.node.Node;

import java.util.Objects;

/**
 * A runtime exception thrown when a WhatsApp protocol node request does not receive a response
 * within the expected timeout period.
 *
 * <p>This exception occurs during socket communication when a {@link Node} is sent to the WhatsApp
 * server but no response is received within the configured timeout duration (typically 60 seconds).
 * The exception captures the original node that timed out, which can be useful for debugging
 * and error handling purposes.
 *
 * <p>Common scenarios that may trigger this exception include:
 * <ul>
 *   <li>Network connectivity issues preventing communication with WhatsApp servers</li>
 *   <li>Server-side delays or unavailability</li>
 *   <li>Invalid or malformed requests that the server ignores</li>
 *   <li>Authentication or session problems</li>
 * </ul>
 *
 * @see Node
 */
public class NodeTimeoutException extends RuntimeException {
    private final Node node;

    /**
     * Constructs a new {@code NodeTimeoutException} with the node that timed out.
     *
     * @param node the WhatsApp protocol node that did not receive a response in time;
     *             must not be {@code null}
     */
    public NodeTimeoutException(Node node) {
        Objects.requireNonNull(node, "node cannot be null");
        this.node = node;
        super(node.toString());
    }

    /**
     * Returns the WhatsApp protocol node that did not receive a response within the timeout period.
     *
     * @return the node that timed out; never {@code null}
     */
    public Node node() {
        return node;
    }
}
