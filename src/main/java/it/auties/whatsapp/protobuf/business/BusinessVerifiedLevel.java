package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of verification that a business account can have
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessVerifiedLevel {
    /**
     * Unknown
     */
    UNKNOWN(0),

    /**
     * Low
     */
    LOW(1),

    /**
     * High
     */
    HIGH(2);

    private final @Getter int index;

    @JsonCreator
    public static BusinessVerifiedLevel forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}