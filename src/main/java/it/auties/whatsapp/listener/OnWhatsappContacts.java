package it.auties.whatsapp.listener;


import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.Contact;

import java.util.Collection;

public interface OnWhatsappContacts
        extends Listener {
    /**
     * Called when the socket receives all the contacts from WhatsappWeb's WebSocket
     *
     * @param whatsapp an instance to the calling api
     * @param contacts the contacts
     */
    @Override
    void onContacts(Whatsapp whatsapp, Collection<Contact> contacts);
}