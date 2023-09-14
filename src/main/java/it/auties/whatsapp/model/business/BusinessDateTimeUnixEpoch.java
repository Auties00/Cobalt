package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a time as a unix epoch
 */
public record BusinessDateTimeUnixEpoch(
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long timestampSeconds
) implements BusinessDateTimeValue {

    /**
     * Returns the timestampSeconds as a zoned date time
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    @Override
    public BusinessDateTimeType dateType() {
        return BusinessDateTimeType.UNIX_EPOCH;
    }
}
