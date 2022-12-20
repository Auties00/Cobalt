package it.auties.whatsapp.listener;


import it.auties.whatsapp.model.contact.Contact;

import java.util.Collection;

public interface OnContacts
        extends Listener {
    /**
     * Called when the socket receives all the contacts from WhatsappWeb's WebSocket
     *
     * @param contacts the contacts
     */
    @Override
    void onContacts(Collection<Contact> contacts);
}