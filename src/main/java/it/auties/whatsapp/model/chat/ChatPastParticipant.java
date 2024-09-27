package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * Class representing a past participant in a chat
 */
@ProtobufMessage(name = "PastParticipant")
public record ChatPastParticipant(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid,
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        Reason reason,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long timestampSeconds
) {

    /**
     * Returns when the past participant left the chat
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    /**
     * Enum representing the errorReason for a past participant leaving the chat.
     */
    @ProtobufEnum(name = "PastParticipant.LeaveReason")
    public enum Reason {
        /**
         * The past participant left the chat voluntarily.
         */
        LEFT(0),
        /**
         * The past participant was removed from the chat.
         */
        REMOVED(1);

        final int index;

        Reason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}