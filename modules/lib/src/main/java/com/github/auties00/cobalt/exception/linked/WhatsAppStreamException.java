package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * Sealed root for problems detected in the WhatsApp protocol stream
 * carried over the WebSocket connection.
 *
 * <p>WhatsApp speaks an XMPP-flavored protocol where every message is a
 * stanza (a stanza with a tag, attributes, and child content). Stream
 * exceptions cover the layer that frames, encodes, and correlates those
 * nodes. Two concrete failure modes exist: a stanza that arrives in an
 * unparseable shape ({@link MalformedNode}) and a request whose response
 * never arrives ({@link NodeTimeout}).
 *
 * @apiNote
 * Raised by the stanza pipeline. {@link NodeTimeout} reports
 * {@link WhatsAppLinkedClientErrorResult#RECONNECT} because an in-flight request
 * was lost and the channel is re-established to clear the correlation state, while
 * {@link MalformedNode} reports {@link WhatsAppLinkedClientErrorResult#DISCARD}:
 * inbound stanzas are decoded frame-by-frame, so one unparseable stanza cannot
 * affect the frames that follow, and a configured
 * {@code WhatsAppClientErrorHandler} drops it while keeping the session running
 * (it may still override the verdict to reconnect).
 *
 * @implNote
 * This implementation returns {@link WhatsAppLinkedClientErrorResult#RECONNECT} as
 * the default; {@link MalformedNode} overrides it to
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}.
 *
 * @see MalformedNode
 * @see NodeTimeout
 */
public sealed class WhatsAppStreamException extends WhatsAppLinkedException
        permits WhatsAppStreamException.MalformedNode, WhatsAppStreamException.NodeTimeout {

    /**
     * Constructs a new stream exception with no detail message.
     */
    public WhatsAppStreamException() {
        super();
    }

    /**
     * Constructs a new stream exception with the specified detail message.
     *
     * @param message the detail message describing the stream error
     */
    public WhatsAppStreamException(String message) {
        super(message);
    }

    /**
     * Constructs a new stream exception with the specified detail message and cause.
     *
     * @param message the detail message describing the stream error
     * @param cause   the underlying cause of this exception
     */
    public WhatsAppStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new stream exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public WhatsAppStreamException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link WhatsAppLinkedClientErrorResult#RECONNECT}
     * as the default for a stream fault, inherited by {@link NodeTimeout} (a lost
     * in-flight request), and re-opens the channel with fresh Noise state,
     * collapsing to WhatsApp Web's {@code CLOSE_SOCKET} resolution.
     * {@link MalformedNode} overrides it to
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.RECONNECT;
    }

    /**
     * Thrown when a stanza received from the server is structurally
     * invalid.
     *
     * <p>The decoder raises this when a stanza is truncated, has a missing
     * required attribute, has the wrong content shape for its tag, or
     * otherwise cannot be parsed into a {@link Stanza}.
     *
     * @apiNote
     * Raised locally on a decode failure. Because inbound stanzas are decoded one
     * per datagram frame, an unparseable stanza is contained to its own frame and
     * does not desync the ones that follow, so {@link #toErrorResult()} defaults to
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: the offending stanza is
     * dropped and the session keeps running. A configured
     * {@code WhatsAppClientErrorHandler} may still override the verdict to
     * reconnect.
     *
     * @implNote
     * This implementation raises the exception rather than emitting an
     * inline NACK, leaving the recovery policy to the configurable error
     * handler.
     */
    public static final class MalformedNode extends WhatsAppStreamException {
        /**
         * Constructs a new malformed stanza exception with no detail message.
         */
        public MalformedNode() {
            super();
        }

        /**
         * Constructs a new malformed stanza exception with the specified message.
         *
         * @param message the detail message describing why the stanza is malformed
         */
        public MalformedNode(String message) {
            super(message);
        }

        /**
         * Constructs a new malformed stanza exception with the specified message and cause.
         *
         * @param message the detail message describing why the stanza is malformed
         * @param cause   the underlying cause
         */
        public MalformedNode(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD}:
         * frame-bounded decoding isolates a malformed stanza to its own datagram, so
         * the stanza is dropped without tearing down the session, mirroring WA Web's
         * {@code WAComms.parseAndHandleStanza}, which logs a stanza-parse failure and
         * keeps reading.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.DISCARD;
        }
    }

    /**
     * Thrown when a request stanza never receives the matching response
     * within the expected window.
     *
     * <p>WhatsApp uses a request-response pattern where each outgoing
     * stanza is tagged with an id and the server eventually returns a
     * stanza carrying the same id. This exception marks an id whose
     * response did not arrive before the timeout fired.
     *
     * @apiNote
     * Raised on a request timeout; {@link #node()} returns the original
     * request stanza so the caller can log or retry the operation.
     *
     * @see Stanza
     */
    public static final class NodeTimeout extends WhatsAppStreamException {
        /**
         * The request stanza that did not receive a response.
         */
        private final Stanza stanza;

        /**
         * Constructs a new stanza timeout exception with the stanza that timed out.
         *
         * @param stanza the stanza that did not receive a response in time
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        public NodeTimeout(Stanza stanza) {
            super("Stanza timeout: " + Objects.requireNonNull(stanza, "stanza cannot be null"));
            this.stanza = stanza;
        }

        /**
         * Constructs a new stanza timeout exception with a custom message and the timed-out stanza.
         *
         * @param message the detail message describing the timeout condition
         * @param stanza    the stanza that did not receive a response in time
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        public NodeTimeout(String message, Stanza stanza) {
            super(message);
            this.stanza = Objects.requireNonNull(stanza, "stanza cannot be null");
        }

        /**
         * Constructs a new stanza timeout exception with a message, cause, and the timed-out stanza.
         *
         * @param message the detail message describing the timeout condition
         * @param stanza    the stanza that did not receive a response in time
         * @param cause   the underlying cause of the timeout
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        public NodeTimeout(String message, Stanza stanza, Throwable cause) {
            super(message, cause);
            this.stanza = Objects.requireNonNull(stanza, "stanza cannot be null");
        }

        /**
         * Returns the stanza that did not receive a response within the
         * timeout.
         *
         * @return the timed-out request stanza, never {@code null}
         */
        public Stanza node() {
            return stanza;
        }
    }
}
