package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * Class representing a past participant in a chat
 */
public record PastParticipant(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        ContactJid jid,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        ParticipantLeaveReason reason,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long timestampSeconds
) implements ProtobufMessage {

    /**
     * Returns when the past participant left the chat
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }
}