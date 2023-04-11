package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

public interface onWhatsappUserAboutChange extends Listener {
    /**
     * Called when the companion's status changes
     *
     * @param whatsapp an instance to the calling api
     * @param oldAbout the non-null old about
     * @param newAbout the non-null new about
     */
    void onUserAboutChange(Whatsapp whatsapp, String oldAbout, String newAbout);
}
