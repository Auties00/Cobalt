package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of parameters that can be
 * wrapped
 */
public enum BusinessLocalizableParameterType implements ProtobufEnum {
    /**
     * No parameter
     */
    NONE(0),
    /**
     * Currency parameter
     */
    CURRENCY(2),
    /**
     * Date time parameter
     */
    DATE_TIME(3);

    final int index;

    BusinessLocalizableParameterType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
