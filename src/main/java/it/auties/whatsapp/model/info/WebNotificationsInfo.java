package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@ProtobufMessage(name = "WebNotificationsInfo")
public record WebNotificationsInfo(
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long timestampSeconds,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int unreadChats,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int notifyMessageCount,
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        List<ChatMessageInfo> notifyMessages
) implements Info {
    /**
     * Returns when the notification was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }
}
