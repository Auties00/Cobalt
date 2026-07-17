package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallOfferNotice onCallOfferNotice} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallOfferNoticeListener extends LinkedListener {
    /**
     * Notifies the listener of an offer notice, which the server sends to
     * inform the device about a call offer that arrived while it was
     * offline.
     *
     * <p>The call itself is also propagated through the regular
     * {@link LinkedCallListener#onCall onCall} flow, so most listeners do not need
     * to override this method.
     *
     * @param whatsapp the client emitting the event
     * @param call     the offer-notice call descriptor
     */
    void onCallOfferNotice(LinkedWhatsAppClient whatsapp, IncomingCall call);
}
