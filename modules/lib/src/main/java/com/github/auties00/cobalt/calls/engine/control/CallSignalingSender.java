package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallStanza;

/**
 * Dispatches an in call control action out to the signaling wire.
 *
 * <p>An in call control operation builds a typed {@link CallMessage} action and hands it to this seam to be
 * put on the wire. The seam hides where the stanza id assignment, the {@code <call>} envelope wrapping, and
 * the socket write happen: the lifecycle layer that owns a call's dispatcher implements it by wrapping the
 * action through {@link CallStanza#toCall(CallMessage, com.github.auties00.cobalt.wire.core.jid.Jid, String)}
 * with a fresh dispatcher id and writing the result to the Linked socket without waiting for a reply. A
 * controller therefore never needs the call's recipient or the dispatcher, only the typed action and this
 * sink.
 *
 * <p>The send carries no return value and signals no acknowledgement, matching the in call action plane
 * where the server stops retransmitting on its own {@code <ack class="call">} and the engine does not block
 * a control operation on a reply. Operations that instead need a request and reply IQ (the call link query
 * and the waiting room admit and deny, which the relay answers on a typed ack) do not use this seam; they
 * go through their own request senders.
 *
 * @implSpec An implementation MUST address the action to the call's current signaling recipient, assign a
 * fresh dispatcher stanza id, and dispatch it without blocking the caller on a reply. It MUST NOT throw for
 * a transport that is momentarily unavailable; a control action that cannot be sent is dropped, since the
 * control plane is best effort.
 * @see CallMessage
 * @see CallStanza
 */
@FunctionalInterface
public interface CallSignalingSender {
    /**
     * Dispatches an in call control action onto the wire without waiting for a reply.
     *
     * @implSpec An implementation MUST wrap the action in the {@code <call>} envelope with the call's
     * recipient and a fresh dispatcher id and write it without blocking on a reply.
     * @param message the typed in call control action to send; never {@code null}
     */
    void send(CallMessage message);
}
