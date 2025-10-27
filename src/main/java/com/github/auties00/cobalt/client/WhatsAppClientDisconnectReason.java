package com.github.auties00.cobalt.client;

/**
 * Defines the various reasons for which a WhatsApp session can be terminated.
 * This enumeration is used to indicate the cause of disconnection when a session ends,
 * which helps with proper handling of reconnection logic and user notifications.
 */
public enum WhatsAppClientDisconnectReason {
    /**
     * Indicates a normal disconnection initiated by the user or system.
     * This is the default reason when no specific cause is identified.
     */
    DISCONNECTED,

    /**
     * Indicates that the session is being terminated for reconnection.
     * This typically happens during network changes or when refreshing the connection.
     * The application should attempt to establish a new connection when this reason is given.
     */
    RECONNECTING,

    /**
     * Indicates that the user has explicitly logged out of the WhatsApp session.
     * This requires the user to re-authenticate before establishing a new connection.
     * All session credentials should be cleared when this reason is encountered.
     */
    LOGGED_OUT,

    /**
     * Indicates that the account has been banned from using WhatsApp.
     * This is a terminal state that requires user intervention with WhatsApp support.
     * No automatic reconnection should be attempted when this reason is given.
     */
    BANNED
}