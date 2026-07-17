package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.setting.privacy.OptOutEntry;

import java.util.List;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onOptOutList onOptOutList} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedOptOutListListener extends LinkedListener {
    /**
     * Notifies the listener that the marketing-message opt-out list
     * for one category was refreshed against the server.
     *
     * @apiNote
     * Fires each time
     * {@link LinkedWhatsAppClient#refreshOptOutList(String)} commits a
     * fresh server-authoritative view of that category. Use to redraw
     * the marketing-message opt-out settings surface against the new
     * authoritative set.
     *
     * @param whatsapp the client emitting the event
     * @param category the opt-out category that was refreshed
     * @param entries  the new authoritative entries for that
     *                 category; may be empty
     */
    void onOptOutList(LinkedWhatsAppClient whatsapp, String category, List<OptOutEntry> entries);
}
