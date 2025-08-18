package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a time as a unix epoch
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeUnixEpoch")
public final class HighlyStructuredDateTimeUnixEpoch implements HighlyStructuredDateTimeValue {
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    final long timestampSeconds;

    HighlyStructuredDateTimeUnixEpoch(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    /**
     * Returns the timestampSeconds as a zoned date time
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    @Override
    public Type dateType() {
        return Type.UNIX_EPOCH;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredDateTimeUnixEpoch that
                && timestampSeconds == that.timestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampSeconds);
    }

    @Override
    public String toString() {
        return "HighlyStructuredDateTimeUnixEpoch[" +
                "timestampSeconds=" + timestampSeconds + ']';
    }
}