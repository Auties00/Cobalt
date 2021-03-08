package it.auties.whatsapp4j.socket;

/**
 * The constants of this enumerated type describe the various states that {@link WhatsappWebSocket} can be in
 */
public enum SocketState {
    NOTHING,
    SENT_INITIAL_MESSAGE,
    WAITING_FOR_LOGIN,
    SOLVE_CHALLENGE,
    SENT_CHALLENGE,
    LOGGED_IN
}
