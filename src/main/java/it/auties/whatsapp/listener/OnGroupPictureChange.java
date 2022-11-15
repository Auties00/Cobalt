package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.chat.Chat;

public interface OnGroupPictureChange extends Listener {
    /**
     * Called when a group's picture changes
     *
     * @param group the group whose pic changed
     */
    @Override
    void onGroupPictureChange(Chat group);
}
