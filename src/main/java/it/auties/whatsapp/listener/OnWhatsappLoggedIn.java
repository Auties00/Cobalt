package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;

public interface OnWhatsappLoggedIn extends Listener {
    /**
     * Called when {@link Socket} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link OnChats#onChats()} and {@link OnContacts#onContacts()} should be used.
     */
    void onLoggedIn(Whatsapp whatsapp);
}

