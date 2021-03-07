package it.auties.whatsapp4j.model;

/**
 * The constants of this enumerated type describe the various types of mute a {@link WhatsappMute} can describe
 */
public enum WhatsappMuteType {
    /**
     * This constant describes a {@link WhatsappMute} that holds a time greater than 0
     * Simply put, {@link WhatsappMute#time()} > 0
     */
    MUTED_FOR_TIMEFRAME,

    /**
     * This constant describes a {@link WhatsappMute} that holds a time equal to -1
     * Simply put, {@link WhatsappMute#time()} == -1
     */
    MUTED_INDEFINITELY,

    /**
     * This constant describes a {@link WhatsappMute} that holds a time equal to 0
     * Simply put, {@link WhatsappMute#time()} == 0
     */
    NOT_MUTED
}
