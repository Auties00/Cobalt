package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

import java.net.URI;

public interface OnWhatsappProfilePictureChanged extends Listener {
    /**
     * Called when the companion's picture changes
     *
     * @param whatsapp   an instance to the calling api
     * @param oldPicture the non-null old picture
     * @param newPicture the non-null new picture
     */
    @Override
    void onProfilePictureChanged(Whatsapp whatsapp, URI oldPicture, URI newPicture);
}
