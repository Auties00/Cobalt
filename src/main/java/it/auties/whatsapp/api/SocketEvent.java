package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various types of events regarding a socket
 */
public enum SocketEvent {
    /**
     * Called when the socket is opened
     */
    OPEN,

    /**
     * Called when the socket is closed
     */
    CLOSE,

    /**
     * Called when an unexpected error is thrown, can be used as a safety mechanism
     */
    ERROR,

    /**
     * Called when a ping is sent
     */
    PING,

    /**
     * Called when the socket is paused because of a network issue
     */
    PAUSED
}
