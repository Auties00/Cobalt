package it.auties.whatsapp.listener;

import it.auties.whatsapp.socket.Socket;

public interface OnLoggedIn extends Listener {
    /**
     * Called when {@link Socket} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link OnChats#onChats()} and {@link OnContacts#onContacts()} should be used.
     */
    @Override
    void onLoggedIn();
}

