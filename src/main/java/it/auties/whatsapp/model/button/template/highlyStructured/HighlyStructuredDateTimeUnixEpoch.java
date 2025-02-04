package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a time as a unix epoch
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeUnixEpoch")
public record HighlyStructuredDateTimeUnixEpoch(
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long timestampSeconds
) implements HighlyStructuredDateTimeValue {

    /**
     * Returns the timestampSeconds as a zoned date time
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    @Override
    public Type dateType() {
        return Type.UNIX_EPOCH;
    }
}
