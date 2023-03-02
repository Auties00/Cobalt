package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of content that an interactive
 * message can wrap
 */
@AllArgsConstructor
@Accessors(fluent = true)
@ProtobufName("InteractiveMessageType")
public enum InteractiveMessageContentType implements ProtobufMessage {
    /**
     * No content
     */
    NONE(0),
    /**
     * Shop
     */
    SHOP(1),
    /**
     * Collection
     */
    COLLECTION(2),
    /**
     * Native flow
     */
    NATIVE_FLOW(3);

    @Getter
    private final int index;

    @JsonCreator
    public static InteractiveMessageContentType of(int index) {
        return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(NONE);
    }
}
