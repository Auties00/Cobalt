package com.github.auties00.cobalt.exception;

import com.github.auties00.cobalt.model.proto.jid.Jid;

/**
 * Signals that an attempt to parse or construct a JID  has failed
 * due to invalid format or children.
 *
 * <p>This exception is typically thrown when:
 * <ul>
 *   <li>A JID string contains unexpected characters or tokens</li>
 *   <li>Required JID components are missing or malformed</li>
 *   <li>Numeric values in the JID are out of valid range</li>
 *   <li>The JID structure does not conform to the expected format</li>
 * </ul>
 *
 * @see Jid
 * @see RuntimeException
 */
public class MalformedJidException extends RuntimeException {
    /**
     * Constructs a new malformed JID exception with the specified detail message.
     *
     * @param message the detail message explaining why the JID is malformed
     */
    public MalformedJidException(String message) {
        super(message);
    }
}