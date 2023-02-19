package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;

public interface OnContactPictureChange extends Listener {
    /**
     * Called when a contact's profile picture changes
     *
     * @param contact the contact whose pic changed
     */
    @Override
    void onContactPictureChange(Contact contact);
}
