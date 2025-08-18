package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Class representing a past participant in a chat
 */
@ProtobufMessage(name = "PastParticipant")
public final class ChatPastParticipant {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final Reason reason;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long timestampSeconds;

    ChatPastParticipant(Jid jid, Reason reason, long timestampSeconds) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.timestampSeconds = timestampSeconds;
    }

    public Jid jid() {
        return jid;
    }

    /**
     * Returns when the past participant left the chat
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    public Reason reason() {
        return reason;
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatPastParticipant that
                && Objects.equals(jid, that.jid)
                && Objects.equals(reason, that.reason)
                && timestampSeconds == that.timestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, reason, timestampSeconds);
    }

    @Override
    public String toString() {
        return "ChatPastParticipant[" +
                "jid=" + jid + ", " +
                "reason=" + reason + ", " +
                "timestampSeconds=" + timestampSeconds + ']';
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
    }
}