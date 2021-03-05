package it.auties.whatsapp4j.socket;

public enum SocketState {
    NOTHING,
    SENT_INITIAL_MESSAGE,
    WAITING_FOR_LOGIN,
    SOLVE_CHALLENGE,
    SENT_CHALLENGE,
    LOGGED_IN
}
