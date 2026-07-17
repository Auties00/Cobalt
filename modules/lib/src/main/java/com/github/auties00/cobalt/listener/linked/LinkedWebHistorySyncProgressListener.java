package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onWebHistorySyncProgress onWebHistorySyncProgress} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedWebHistorySyncProgressListener extends LinkedListener {
    /**
     * Notifies the listener of progress made by the history-synchronization
     * process.
     *
     * <p>This event is only triggered during initial QR code scanning and
     * history syncing.
     *
     * @param whatsapp   the client emitting the event
     * @param percentage the percentage of synchronization completed
     * @param recent     {@code true} if syncing recent messages,
     *                   {@code false} if syncing older messages
     */
    void onWebHistorySyncProgress(LinkedWhatsAppClient whatsapp, int percentage, boolean recent);
}
