package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of business accounts
 */
public enum BusinessAccountType implements ProtobufEnum {
    /**
     * Enterprise
     */
    ENTERPRISE(0),
    /**
     * Page
     */
    PAGE(1);

    final int index;
    BusinessAccountType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}