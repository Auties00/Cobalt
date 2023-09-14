package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the days of the week
 */
public enum BusinessDayOfWeek implements ProtobufEnum {
    /**
     * Monday
     */
    MONDAY(1),
    /**
     * Tuesday
     */
    TUESDAY(2),
    /**
     * Wednesday
     */
    WEDNESDAY(3),
    /**
     * Thursday
     */
    THURSDAY(4),
    /**
     * Friday
     */
    FRIDAY(5),
    /**
     * Saturday
     */
    SATURDAY(6),
    /**
     * Sunday
     */
    SUNDAY(7);

    final int index;

    BusinessDayOfWeek(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
