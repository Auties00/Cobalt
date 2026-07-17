package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCall onCall} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallListener extends LinkedListener {
    /**
     * Notifies the listener that an inbound call offer has arrived.
     *
     * <p>The listener must respond by calling
     * {@link LinkedWhatsAppClient#acceptCall(IncomingCall, com.github.auties00.cobalt.calls.stream.AudioOutput, com.github.auties00.cobalt.calls.stream.AudioInput)}
     * or
     * {@link LinkedWhatsAppClient#rejectCall(IncomingCall, com.github.auties00.cobalt.wire.linked.call.CallEndReason)}
     * with the supplied offer within the WhatsApp-imposed offer timeout
     * (about 30 seconds); otherwise the offer expires. The listener is
     * invoked from the call layer's signaling thread, so long-running
     * decisions should hand off to a virtual thread to avoid stalling
     * signaling.
     *
     * @param whatsapp the client emitting the event
     * @param incoming the inbound offer carrying its metadata
     */
    void onCall(LinkedWhatsAppClient whatsapp, IncomingCall incoming);
}
