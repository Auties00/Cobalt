package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

public record NewsletterName(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String text,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        @JsonProperty("update_time")
        long updateTimeSeconds
) implements ProtobufMessage {
    public Optional<ZonedDateTime> updateTime() {
        return Clock.parseSeconds(updateTimeSeconds);
    }
}
