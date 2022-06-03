package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of actors of a business account
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessActorsType implements ProtobufMessage {
    /**
     * Self
     */
    SELF(0),
    /**
     * Bsp
     */
    BSP(1);

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessActorsType forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}
