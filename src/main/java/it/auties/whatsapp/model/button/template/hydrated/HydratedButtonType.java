package it.auties.whatsapp.model.button.template.hydrated;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of buttons that a template can
 * wrap
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum HydratedButtonType implements ProtobufMessage {
    /**
     * No button
     */
    NONE(0),
    /**
     * Quick reply button
     */
    QUICK_REPLY(1),
    /**
     * Url button
     */
    URL(2),
    /**
     * Call button
     */
    CALL(3);

    @Getter
    private final int index;

    @JsonCreator
    public static HydratedButtonType of(int index) {
        return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(HydratedButtonType.NONE);
    }
}