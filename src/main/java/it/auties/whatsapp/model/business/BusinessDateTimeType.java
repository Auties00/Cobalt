package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various type of date types that a date time can wrap
 */
public enum BusinessDateTimeType implements ProtobufEnum {
    /**
     * No date
     */
    NONE(0),
    /**
     * Component date
     */
    COMPONENT(1),
    /**
     * Unix epoch date
     */
    UNIX_EPOCH(2);


    final int index;
    BusinessDateTimeType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
