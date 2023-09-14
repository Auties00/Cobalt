package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of verification that a business
 * account can have
 */
public enum BusinessVerifiedLevel implements ProtobufEnum {
    /**
     * Unknown
     */
    UNKNOWN(0),

    /**
     * Low
     */
    LOW(1),

    /**
     * High
     */
    HIGH(2);

    final int index;

    BusinessVerifiedLevel(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}