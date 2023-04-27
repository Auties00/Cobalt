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
     * This is the default setting for the web client
     * This is also the recommended setting
     */
    STANDARD,

    /**
     * This will contain most of your messages
     * Unless you 100% know what you are doing don't use this
     * It consumes a lot of system resources
     */
    EXTENDED,
}
