package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;

public interface OnNewContact extends Listener {
    /**
     * Called when the socket receives a new contact.
     * There isn't an overloaded method with a Whatsapp parameter due to technical limitations.
     *
     * @param contact the new contact
     */
    @Override
    void onNewContact(Contact contact);
}