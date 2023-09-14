package it.auties.whatsapp.model.media;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


public record MediaData(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String localPath,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        Long mediaKeyTimestampSeconds,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] fileSha256,
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] fileEncSha256,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String directPath
) implements ProtobufMessage {
    /**
     * Returns the media key timestampSeconds
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Clock.parseSeconds(mediaKeyTimestampSeconds);
    }
}