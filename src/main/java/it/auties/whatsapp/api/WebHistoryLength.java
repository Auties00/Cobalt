package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various chat history's codeLength that Whatsapp
 * can send on the first login attempt
 */
public enum WebHistoryLength {
    /**
     * Discards history
     * This will save a lot of system resources, but you won't have access to messages sent before the session creation
     */
    ZERO,

    /**
     * Three months worth of chat
     */
    THREE_MONTHS,

    /**
     * One year worth of chats
     */
    ONE_YEAR,
}
