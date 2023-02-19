package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;

public interface OnWhatsappGroupPictureChange extends Listener {
    /**
     * Called when a group's picture changes
     *
     * @param whatsapp an instance to the calling api
     * @param group    the group whose pic changed
     */
    @Override
    void onGroupPictureChange(Whatsapp whatsapp, Chat group);
}
