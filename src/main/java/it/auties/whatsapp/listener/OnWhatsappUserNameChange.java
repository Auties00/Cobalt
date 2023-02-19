package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappUserNameChange extends Listener {
    /**
     * Called when the companion's name changes
     *
     * @param whatsapp an instance to the calling api
     * @param oldName  the non-null old name
     * @param newName  the non-null new name
     */
    @Override
    void onUserNameChange(Whatsapp whatsapp, String oldName, String newName);
}
