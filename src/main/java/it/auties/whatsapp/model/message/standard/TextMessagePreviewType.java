package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of previuew that a
 * {@link TextMessage} can provide.
 */
public enum TextMessagePreviewType implements ProtobufEnum {
    /**
     * No preview
     */
    NONE(0),

    /**
     * Video preview
     */
    VIDEO(1);

    final int index;

    TextMessagePreviewType(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
