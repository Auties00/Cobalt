package it.auties.whatsapp.model.message.model;

/**
 * The constants of this enumerated type describe the various categories of messages that a {@link MessageContainer} can wrap
 */
public enum MessageCategory {
    /**
     * Server message
     */
    EMPTY,

    /**
     * Device message
     */
    BUTTON,

    /**
     * PAYMENT message
     */
    PAYMENT,

    /**
     * Server message
     */
    SERVER,

    /**
     * Device message
     */
    DEVICE,

    /**
     * Standard message
     */
    STANDARD
}