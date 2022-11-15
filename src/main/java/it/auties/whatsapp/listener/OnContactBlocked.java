package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;

public interface OnContactBlocked extends Listener{
    /**
     * Called when a contact is blocked or unblocked
     *
     * @param contact the non-null contact
     */
    @Override
    void onContactBlocked(Contact contact);
}
