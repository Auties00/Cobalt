package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappUserStatusChange
        extends Listener {
    /**
     * Called when the companion's status changes
     *
     * @param whatsapp  an instance to the calling api
     * @param oldStatus the non-null old status
     * @param newStatus the non-null new status
     */
    void onUserStatusChange(Whatsapp whatsapp, String oldStatus, String newStatus);
}
