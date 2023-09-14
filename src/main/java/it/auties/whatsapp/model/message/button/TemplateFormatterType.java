package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constant of this enumerated type define the various of types of visual formats for a
 * {@link TemplateMessage}
 */
public enum TemplateFormatterType implements ProtobufEnum {
    /**
     * No format
     */
    NONE(0),
    /**
     * Four row template
     */
    FOUR_ROW(1),
    /**
     * Hydrated four row template
     */
    HYDRATED_FOUR_ROW(2),
    /**
     * Interactive message
     */
    INTERACTIVE(3);

    final int index;
    TemplateFormatterType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
