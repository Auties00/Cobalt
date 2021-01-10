package it.auties.whatsapp4j.api;

public enum WhatsappState {
    NOTHING,
    SENT_INITIAL_MESSAGE,
    WAITING_FOR_LOGIN,
    SOLVE_CHALLENGE,
    SENT_CHALLENGE,
    LOGGED_IN,
    LOGGED_OFF
}
