package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of title that a template can
 * wrap
 */
public enum HydratedFourRowTemplateTitleType implements ProtobufEnum {
    /**
     * No title
     */
    NONE(0),
    /**
     * Document title
     */
    DOCUMENT(1),
    /**
     * Text title
     */
    TEXT(2),
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

    final int index;
    HydratedFourRowTemplateTitleType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
