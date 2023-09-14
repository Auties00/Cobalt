package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of actors of a business account
 */
public enum BusinessActorsType implements ProtobufEnum {
    /**
     * Self
     */
    SELF(0),
    /**
     * Bsp
     */
    BSP(1);

    final int index;
    BusinessActorsType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}