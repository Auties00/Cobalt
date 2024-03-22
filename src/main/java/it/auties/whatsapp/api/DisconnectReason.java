package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various reasons for which a session can be
 * terminated
 */
public enum DisconnectReason {
    /**
     * Default errorReason
     */
    DISCONNECTED,

    /**
     * Reconnect
     */
    RECONNECTING,

    /**
     * Logged out
     */
    LOGGED_OUT,

    /**
     * Session restore
     */
    RESTORE,

    /**
     * Ban
     */
    BANNED
}
