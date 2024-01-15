package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * Class representing a past participant in a chat
 */
@ProtobufMessageName("PastParticipant")
public record GroupPastParticipant(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Reason reason,
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

    /**
     * Enum representing the errorReason for a past participant leaving the chat.
     */
    @ProtobufMessageName("PastParticipant.LeaveReason")
    public enum Reason implements ProtobufEnum {
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