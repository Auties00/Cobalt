package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.controller.Store;

public interface OnWhatsappStatus extends Listener {
    /**
     * Called when {@link Socket} receives all the status updated from WhatsappWeb's Socket.
     * To access this data use {@link Store#status()}.
     */
    void onStatus(Whatsapp whatsapp);
}