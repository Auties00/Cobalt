package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * Class representing a past participant in a chat.
 *
 * @author [Author Name]
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PastParticipant")
public class PastParticipant implements ProtobufMessage {
    /**
     * The jid of the past participant
     */
    @ProtobufProperty(index = 1, name = "userJid", type = STRING)
    private ContactJid jid;

    /**
     * The errorReason for the past participant leaving the chat
     */
    @ProtobufProperty(index = 2, name = "leaveReason", type = MESSAGE)
    private LeaveReason reason;

    /**
     * The timestamp of when the past participant left the chat
     */
    @ProtobufProperty(index = 3, name = "leaveTs", type = UINT64)
    private long timestampSeconds;

    /**
     * Returns when the past participant left the chat
     *
     * @return a timestamp
     */
    public ZonedDateTime timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    /**
     * Enum representing the errorReason for a past participant leaving the chat.
     */
    @AllArgsConstructor
    public enum LeaveReason implements ProtobufMessage {
        /**
         * The past participant left the chat voluntarily.
         */
        LEFT(0),
        /**
         * The past participant was removed from the chat.
         */
        REMOVED(1);

        /**
         * Getter for the index of the leave errorReason.
         */
        @Getter
        private final int index;
    }
}