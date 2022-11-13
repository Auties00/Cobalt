package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnNewContact extends Listener {
    /**
     * Called when {@link SocketHandler} receives a new contact
     *
     * @param contact the new contact
     */
    @Override
    void onNewContact(Contact contact);
}