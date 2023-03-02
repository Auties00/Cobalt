package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of title that a template can
 * have
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum FourRowTemplateTitleType implements ProtobufMessage {
    /**
     * No title
     */
    NONE(0),
    /**
     * Document title
     */
    DOCUMENT(1),
    /**
     * Highly structured message title
     */
    HIGHLY_STRUCTURED(2),
    /**
     * Image title
     */
    IMAGE(3),
    /**
     * Video title
     */
    VIDEO(4),
    /**
     * Location title
     */
    LOCATION(5);

    @Getter
    private final int index;

    @JsonCreator
    public static FourRowTemplateTitleType of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(FourRowTemplateTitleType.NONE);
    }
}
