package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various types of status of a {@link Message}
 */
@ProtobufEnum
public enum MessageStatus {
    /**
     * Erroneous status(no ticks)
     */
    ERROR(0),
    /**
     * Pending status(no ticks)
     */
    PENDING(1),
    /**
     * Acknowledged by the server(one tick)
     */
    SERVER_ACK(2),
    /**
     * Delivered(two ticks)
     */
    DELIVERED(3),
    /**
     * Read(two blue ticks)
     */
    READ(4),
    /**
     * Played(two blue ticks)
     */
    PLAYED(5);

    final int index;

    MessageStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public static Optional<MessageStatus> of(String name) {
        return name == null ? Optional.empty() : Arrays.stream(values())
                .filter(entry -> name.toLowerCase().contains(entry.name().toLowerCase()))
                .findFirst();
    }

    public int index() {
        return this.index;
    }
}