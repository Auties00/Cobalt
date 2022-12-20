package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.Contact;

public interface OnWhatsappContactBlocked
        extends Listener {
    /**
     * Called when a contact is blocked or unblocked
     *
     * @param whatsapp an instance to the calling api
     * @param contact  the non-null contact
     */
    @Override
    void onContactBlocked(Whatsapp whatsapp, Contact contact);
}
