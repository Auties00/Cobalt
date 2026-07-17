package com.github.auties00.cobalt.listener.linked.internal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * The internal listener that delivers a peer's reciprocal trusted-contact token to the client.
 *
 * <p>{@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#queryTrustedContactToken(com.github.auties00.cobalt.wire.core.jid.JidProvider)},
 * when no token is cached, vouches for a peer and then blocks until the peer's token is pushed back;
 * the privacy-token stream handler stores the delivered token on the peer's chat and then notifies the
 * client's always-registered instance of this listener, which wakes the blocked query so it completes.
 */
public non-sealed interface LinkedTrustedContactTokenListener extends InternalLinkedListener {
    /**
     * Handles a peer's reciprocal trusted-contact token after the privacy-token stream handler has
     * stored it on the peer's chat.
     *
     * <p>The token has already been written to the chat record when this runs, so an implementation
     * that resolves the token through the store observes the stored value; the {@code peer} and
     * {@code token} are passed for convenience and to wake a call blocked on this exact peer.
     *
     * @param peer  the user JID of the peer whose token was delivered; never {@code null}
     * @param token the raw trusted-contact token bytes; never {@code null}
     */
    void onTrustedContactTokenReceived(Jid peer, byte[] token);
}
