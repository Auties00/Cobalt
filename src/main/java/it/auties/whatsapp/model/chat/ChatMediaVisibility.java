package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of media visibility that can be set for a chat
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatMediaVisibility implements ProtobufMessage {
    /**
     * Default
     */
    DEFAULT(0),

    /**
     * Off
     */
    OFF(1),

    /**
     * On
     */
    ON(2);

    @Getter
    private final int index;

    @JsonCreator
    public static ChatMediaVisibility of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}
