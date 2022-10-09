package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of verification that a business account can have
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessVerifiedLevel implements ProtobufMessage {
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

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessVerifiedLevel forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}
