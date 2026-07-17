package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onContactBlocked onContactBlocked} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedContactBlockedListener extends LinkedListener {
    /**
     * Notifies the listener that a single contact's blocked state was
     * toggled.
     *
     * @apiNote
     * Fires after a local {@link LinkedWhatsAppClient#blockContact(JidProvider)}
     * or {@link LinkedWhatsAppClient#unblockContact(JidProvider)} succeeds, so
     * the UI can give immediate feedback on the action the user just
     * took. Bulk reconciliations of the Blocked Contacts privacy list
     * surface through
     * {@link LinkedBlockedContactsListener#onBlockedContacts onBlockedContacts} instead.
     *
     * @param whatsapp the client emitting the event
     * @param contact  the contact that was blocked or unblocked
     */
    void onContactBlocked(LinkedWhatsAppClient whatsapp, Jid contact);
}
