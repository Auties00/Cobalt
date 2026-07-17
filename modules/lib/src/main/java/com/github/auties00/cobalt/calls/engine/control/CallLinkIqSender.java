package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Sends a call link or waiting room IQ request and returns the reply the controller parses.
 *
 * <p>The call link query, join, and edit operations, together with the waiting room admit and deny
 * operations, are request reply IQ exchanges rather than fire and forget sends: each is an IQ addressed to
 * the {@code call} service that the relay answers with a typed acknowledgement carried in the reply. This
 * seam captures that round trip. It accepts the typed request {@link CallMessage} and returns the reply
 * {@link Stanza} that the controller parses into the operation's ack, hiding the IQ envelope construction,
 * the {@code to="call"} addressing, the per operation request type, and the blocking wait for the reply.
 * Each operation carries its own IQ request type: the call link query type {@code 0x84}, the waiting room
 * admit type {@code 0x47}, and the waiting room deny type {@code 0x49}.
 *
 * <p>The exchange blocks the calling virtual thread until the reply arrives, matching the threading model in
 * which a request reply IQ is a plain blocking call on a virtual thread rather than a future.
 *
 * @implSpec An implementation MUST wrap the request in the IQ envelope addressed to the {@code call} service
 * with the request type the operation uses, block the calling thread until the reply arrives, and return the
 * reply stanza. It MAY throw to signal a transport failure or a server error reply; a controller treats a
 * throw as the operation failing.
 * @see CallMessage
 */
@FunctionalInterface
public interface CallLinkIqSender {
    /**
     * Dispatches a call link or waiting room IQ request and returns its reply.
     *
     * @implSpec An implementation MUST block until the reply arrives and return the reply stanza, or throw on
     * a transport or server failure.
     * @param request the typed request to send; never {@code null}
     * @return the reply stanza the caller parses into the operation's ack; never {@code null}
     */
    Stanza sendForReply(CallMessage request);
}
