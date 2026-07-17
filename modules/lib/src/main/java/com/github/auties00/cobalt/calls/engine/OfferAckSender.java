package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Sends an outbound {@code <call><offer>} stanza and returns the synchronous call ack the server replies
 * with.
 *
 * <p>The offer is the one signaling leg whose acknowledgement is synchronous and load bearing: the server
 * replies to the offer stanza with an {@code <ack class="call" type="offer">} as the send call's return
 * value, and that ack carries the caller's own {@code <relay>} block, the per device {@code <voip_settings>}
 * bundles, and the participant roster the caller needs to bring up its media plane. Every other signaling
 * leg (accept, preaccept, reject, terminate) is fire and forget through
 * {@link com.github.auties00.cobalt.calls.platform.VoipHostApi#sendSignaling(Stanza)}, with its
 * acknowledgement arriving later on the inbound path, so only the offer needs this request and response
 * seam.
 *
 * <p>The controller builds the offer stanza and hands it here; the implementer ships it on the client
 * transport and blocks the calling virtual thread for the ack round trip. A NACK is returned as the ack
 * {@link Stanza} the same way a positive ack is, carrying its {@code error} attribute and a relay block
 * with only the denormalised call creator and {@code call-id}; the controller inspects the returned stanza
 * to tell ack from NACK.
 *
 * @apiNote This is an internal engine collaborator, not a public surface; embedders never call it.
 */
@FunctionalInterface
public interface OfferAckSender {
    /**
     * Sends the offer stanza and returns the server's synchronous call ack.
     *
     * <p>The implementer ships the supplied {@code <call>} envelope on the client transport and blocks the
     * calling virtual thread until the matching {@code <ack class="call">} returns. The returned stanza is
     * the ack itself, whether positive (carrying the caller's relay block and settings) or a NACK (carrying
     * an {@code error} attribute), so the controller reads the stanza to classify the result and to extract
     * the relay block for the media plane bring up.
     *
     * @param offerEnvelope the built {@code <call>} envelope nesting the {@code <offer>} action
     * @return the server's {@code <ack class="call">} reply stanza
     * @throws NullPointerException if {@code offerEnvelope} is {@code null}
     * @throws com.github.auties00.cobalt.exception.linked.WhatsAppCallException.DataChannel if the offer could not
     * be sent or no ack arrived
     */
    Stanza sendOfferAndAwaitAck(Stanza offerEnvelope);
}
