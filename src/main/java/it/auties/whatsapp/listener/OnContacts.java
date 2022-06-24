package it.auties.whatsapp.listener;


import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.controller.Store;

public interface OnContacts extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives all the contacts from WhatsappWeb's WebSocket.
     * To access this data use {@link Store#contacts()}.
     */
    void onContacts();
}