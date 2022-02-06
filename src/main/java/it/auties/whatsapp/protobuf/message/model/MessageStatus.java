package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Locale;

/**
 * The constants of this enumerated type describe the various types of status of a {@link Message}
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum MessageStatus {
    /**
     * Unknown
     */
    UNKNOWN(0),

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

    @Getter
    private final int index;

    public static MessageStatus forValue(String name) {
        return name == null ? null : Arrays.stream(values())
                .filter(entry -> name.toLowerCase().contains(entry.name().toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    @JsonCreator
    public static MessageStatus forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(UNKNOWN);
    }
}