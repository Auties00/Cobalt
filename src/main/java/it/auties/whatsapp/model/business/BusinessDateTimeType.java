package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various type of date types that a date time
 * can wrap
 */
@AllArgsConstructor
@Accessors(fluent = true)
@ProtobufName("DatetimeOneofType")
public enum BusinessDateTimeType implements ProtobufMessage {
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

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessDateTimeType of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(BusinessDateTimeType.NONE);
    }
}
