package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of status of a {@link Message}
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum MessageStatus {
    /**
     * Error
     */
    ERROR(0),

    /**
     * Pending
     */
    PENDING(1),

    /**
     * Acknowledged by the server
     */
    SERVER_ACK(2),

    /**
     * Delivered
     */
    DELIVERED(3),

    /**
     * Read
     */
    READ(4),

    /**
     * Played
     */
    PLAYED(5);

    private final @Getter int index;

    @JsonCreator
    public static MessageStatus forName(String encoded){
        return switch (encoded) {
            case null -> DELIVERED;
            case "read", "read-self" -> READ;
            default -> throw new IllegalStateException("Unexpected value: " + encoded);
        };
    }

    @JsonCreator
    public static MessageStatus forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}