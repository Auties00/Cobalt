package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.Contact;

public interface OnProfilePictureChange extends Listener{
    /**
     * Called when a contact's profile picture changes
     *
     * @param contact the contact whose pic changed
     * @param oldPic the old picture, cannot be null
     * @param newPic the new picture, can be null if the old picture was deleted and not replaced
     */
    @Override
    void onProfilePictureChange(Contact contact, byte[] oldPic, byte[] newPic);
}
