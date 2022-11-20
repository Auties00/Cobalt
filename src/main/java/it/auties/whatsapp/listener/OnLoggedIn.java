package it.auties.whatsapp.listener;

import it.auties.whatsapp.socket.SocketHandler;

import java.util.Collection;

public interface OnLoggedIn extends Listener {
    /**
     * Called when the socket successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link OnChats#onChats(Collection)} ()} and {@link OnContacts#onContacts(Collection)} ()} should be used.
     */
    @Override
    void onLoggedIn();
}

