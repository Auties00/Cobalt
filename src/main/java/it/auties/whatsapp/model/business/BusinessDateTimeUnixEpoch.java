package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model class that represents a time as a unix epoch
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class BusinessDateTimeUnixEpoch implements BusinessDateTimeValue {
    /**
     * The timestamp of the date
     */
    @ProtobufProperty(index = 1, type = INT64)
    private long timestampSeconds;

    /**
     * Returns the timestamp as a zoned date time
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
