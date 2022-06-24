package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.contact.Contact;

public interface OnNewContact extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives a new contact
     *
     * @param contact the new contact
     */
    void onNewContact(Contact contact);
}