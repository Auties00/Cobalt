package com.github.auties00.cobalt.exception;

import com.github.auties00.cobalt.client.WhatsAppClientErrorHandler;

/**
 * A runtime exception that is thrown when a malformed or invalid node is encountered in the WhatsApp protocol stream.
 * <p>
 * This exception typically occurs when:
 * <ul>
 *     <li>The XML structure of a received node is not well-formed</li>
 *     <li>A node fails to meet the required structure</li>
 *     <li>The node structure is corrupted or incomplete</li>
 * </ul>
 *
 * @see WhatsAppClientErrorHandler
 */
public class MalformedNodeException extends RuntimeException {
    /**
     * Constructs a new {@code MalformedNodeException} with no detail message.
     * <p>
     * This constructor is typically used when the error context is self-evident
     * from the call stack or when additional details are not available.
     * </p>
     */
    public MalformedNodeException() {
        super();
    }
}