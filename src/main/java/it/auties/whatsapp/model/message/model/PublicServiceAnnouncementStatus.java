package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

@ProtobufMessage(name = "StatusPSA")
public record PublicServiceAnnouncementStatus(
        @ProtobufProperty(index = 44, type = ProtobufType.STRING)
        String campaignId,
        @ProtobufProperty(index = 45, type = ProtobufType.UINT64)
        long campaignExpirationTimestampSeconds
) {
    public Optional<ZonedDateTime> campaignExpirationTimestamp() {
        return Clock.parseSeconds(campaignExpirationTimestampSeconds);
    }
}