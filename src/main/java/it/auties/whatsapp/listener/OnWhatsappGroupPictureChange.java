package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;

public interface OnWhatsappGroupPictureChange extends Listener {
    /**
     * Called when a group's picture changes
     *
     * @param whatsapp an instance to the calling api
     * @param group the group whose pic changed
     * @param oldPic the old picture, cannot be null
     * @param newPic the new picture, can be null if the old picture was deleted and not replaced
     */
    @Override
    void onGroupPictureChange(Whatsapp whatsapp, Chat group, byte[] oldPic, byte[] newPic);
}
