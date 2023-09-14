package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the supported calendar types
 */
public enum BusinessCalendarType implements ProtobufEnum {
    /**
     * Gregorian calendar
     */
    GREGORIAN(1),
    /**
     * Solar calendar
     */
    SOLAR_HIJRI(2);

    final int index;

    BusinessCalendarType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
