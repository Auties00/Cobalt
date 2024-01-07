package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of text preview that can be
 * used
 */
public enum TextPreviewSetting implements ProtobufEnum {
    /**
     * Link previews will be generated. If a message contains an url without a schema(for example
     * wikipedia.com), the message will be autocorrected to include it and a preview will be
     * generated
     */
    ENABLED_WITH_INFERENCE(0),

    /**
     * Link previews will be generated. No inference will be used.
     */
    ENABLED(1),

    /**
     * Link previews will not be generated
     */
    DISABLED(2);

    final int index;

    TextPreviewSetting(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
