package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.socket.Socket;

public interface OnNewContact extends Listener {
    /**
     * Called when {@link Socket} receives a new contact
     *
     * @param contact the new contact
     */
    @Override
    void onNewContact(Contact contact);
}