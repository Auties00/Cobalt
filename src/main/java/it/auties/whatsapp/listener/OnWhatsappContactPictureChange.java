package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.Contact;

public interface OnWhatsappContactPictureChange extends Listener {
    /**
     * Called when a contact's profile picture changes
     *
     * @param whatsapp an instance to the calling api
     * @param contact  the contact whose pic changed
     */
    @Override
    void onContactPictureChange(Whatsapp whatsapp, Contact contact);
}
