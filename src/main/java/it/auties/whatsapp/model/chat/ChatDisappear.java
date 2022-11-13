package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model that represents a chat disappear mode
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ChatDisappear implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = Type.class)
    private Type disappear;

    /**
     * The constants of this enumerated type describe the various actors that can initialize disappearing messages in a chat
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Type implements ProtobufMessage {
        /**
         * Changed in chat
         */
        CHANGED_IN_CHAT(0),

        /**
         * Initiated by me
         */
        INITIATED_BY_ME(1),

        /**
         * Initiated by other
         */
        INITIATED_BY_OTHER(2);

        @Getter
        private final int index;

        @JsonCreator
        public static Type of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
