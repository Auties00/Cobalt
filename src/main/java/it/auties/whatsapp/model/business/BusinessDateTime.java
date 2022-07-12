package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents a time
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(builderMethodName = "newRawDateTimeBuilder")
@Jacksonized
@Accessors(fluent = true)
public class BusinessDateTime implements ProtobufMessage {
    /**
     * The date as a component
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = BusinessDateTimeComponent.class)
    private BusinessDateTimeComponent componentDate;

    /**
     * The date as a unix epoch
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = BusinessDateTimeUnixEpoch.class)
    private BusinessDateTimeUnixEpoch unixEpochDate;

    /**
     * Constructs a new date time using a component
     *
     * @param componentDate the non-null component
     * @return a non-null date time
     */
    public static BusinessDateTime of(@NonNull BusinessDateTimeComponent componentDate) {
        return BusinessDateTime.newRawDateTimeBuilder()
                .componentDate(componentDate)
                .build();
    }

    /**
     * Constructs a new date time using a unix component
     *
     * @param unixEpochDate the non-null unix epoch
     * @return a non-null date time
     */
    public static BusinessDateTime of(@NonNull BusinessDateTimeUnixEpoch unixEpochDate) {
        return BusinessDateTime.newRawDateTimeBuilder()
                .unixEpochDate(unixEpochDate)
                .build();
    }

    /**
     * Returns the type of date that this wrapper wraps
     *
     * @return a non-null date type
     */
    public DateType dateType() {
        if (componentDate != null)
            return DateType.COMPONENT;
        if (unixEpochDate != null)
            return DateType.UNIX_EPOCH;
        return DateType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various type of date types that a date time can wrap
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum DateType implements ProtobufMessage {
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
        public static DateType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(DateType.NONE);
        }
    }
}
