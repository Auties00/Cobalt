package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of {@link ListMessage}
 */
public enum ListMessageType implements ProtobufEnum {
    /**
     * Unknown
     */
    UNKNOWN(0),
    /**
     * Only one option can be selected
     */
    SINGLE_SELECT(1),
    /**
     * A list of products
     */
    PRODUCT_LIST(2);

    final int index;

    ListMessageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
