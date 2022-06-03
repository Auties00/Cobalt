package it.auties.whatsapp.model.chat;

import it.auties.protobuf.api.model.ProtobufMessage;

/**
 * The constants of this enumerated type describe the various types of mute a {@link ChatMute} can describe
 */
public enum ChatMuteType implements ProtobufMessage {
    /**
     * This constant describes a {@link ChatMute} that holds a endTimeStamp greater than 0
     * Simply put, {@link ChatMute#endTimeStamp()} > 0
     */
    MUTED_FOR_TIMEFRAME,

    /**
     * This constant describes a {@link ChatMute} that holds a endTimeStamp equal to -1
     * Simply put, {@link ChatMute#endTimeStamp()} == -1
     */
    MUTED_INDEFINITELY,

    /**
     * This constant describes a {@link ChatMute} that holds a endTimeStamp equal to 0
     * Simply put, {@link ChatMute#endTimeStamp()} == 0
     */
    NOT_MUTED,
}
