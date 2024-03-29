package it.auties.whatsapp.listener;

import java.net.URI;

public interface OnProfilePictureChanged extends Listener {
    /**
     * Called when the companion's picture changes
     *
     * @param oldPicture the non-null old picture
     * @param newPicture the non-null new picture
     */
    @Override
    void onProfilePictureChanged(URI oldPicture, URI newPicture);
}
