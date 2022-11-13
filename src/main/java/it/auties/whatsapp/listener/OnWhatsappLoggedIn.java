package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.socket.SocketHandler;

import java.util.Collection;

public interface OnWhatsappLoggedIn extends Listener {
    /**
     * Called when {@link SocketHandler} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link OnChats#onChats(Whatsapp, Collection)} ()} and {@link OnContacts#onContacts(Whatsapp, Collection)} ()} should be used.
     *
     * @param whatsapp an instance to the calling api
     */
    @Override
    void onLoggedIn(Whatsapp whatsapp);
}

