package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * The constants of this enumerated type describe the various actors that can initialize disappearing messages in a chat
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatDisappear implements ProtobufMessage {
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
    public static ChatDisappear forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }

    @AllArgsConstructor
    @Data
    @Builder
    @Jacksonized
    @Accessors(fluent = true)
    protected static class Linker implements ProtobufMessage{
        @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ChatDisappear.class)
        protected ChatDisappear disappear;

        protected Linker(int index){
            this.disappear = ChatDisappear.forIndex(index);
        }
    }
}
