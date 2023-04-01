package it.auties.whatsapp.model.message.model;

/**
 * The constants of this enumerated type describe the various categories of messages that a
 * {@link MessageContainer} can wrap
 */
public enum MessageCategory {
    /**
     * Device message
     */
    BUTTON,
    /**
     * Payment message
     */
    PAYMENT,
    /**
     * Payment message
     */
    MEDIA,
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