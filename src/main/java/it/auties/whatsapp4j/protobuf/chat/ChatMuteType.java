package it.auties.whatsapp4j.protobuf.chat;

/**
 * The constants of this enumerated type describe the various types of mute a {@link ChatMute} can describe
 */
public enum ChatMuteType {
    /**
     * This constant describes a {@link ChatMute} that holds a time greater than 0
     * Simply put, {@link ChatMute#time()} > 0
     */
    MUTED_FOR_TIMEFRAME,

    /**
     * This constant describes a {@link ChatMute} that holds a time equal to -1
     * Simply put, {@link ChatMute#time()} == -1
     */
    MUTED_INDEFINITELY,

    /**
     * This constant describes a {@link ChatMute} that holds a time equal to 0
     * Simply put, {@link ChatMute#time()} == 0
     */
    NOT_MUTED,

    /**
     * This constant describes a {@link ChatMute} that holds an unknown mute
     */
    UNKNOWN;
}
