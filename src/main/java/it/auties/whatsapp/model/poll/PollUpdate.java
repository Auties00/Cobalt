package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Not currently used, so it's
 * package private
 */
@ProtobufMessage(name = "PollUpdate")
public final class PollUpdate {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey pollUpdateMessageKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final PollUpdateEncryptedOptions vote;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long senderTimestampMilliseconds;

    PollUpdate(ChatMessageKey pollUpdateMessageKey, PollUpdateEncryptedOptions vote, long senderTimestampMilliseconds) {
        this.pollUpdateMessageKey = Objects.requireNonNull(pollUpdateMessageKey, "pollUpdateMessageKey cannot be null");
        this.vote = Objects.requireNonNull(vote, "vote cannot be null");
        this.senderTimestampMilliseconds = senderTimestampMilliseconds;
    }

    public ChatMessageKey pollUpdateMessageKey() {
        return pollUpdateMessageKey;
    }

    public PollUpdateEncryptedOptions vote() {
        return vote;
    }

    public long senderTimestampMilliseconds() {
        return senderTimestampMilliseconds;
    }

    /**
     * Returns when the update was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> senderTimestamp() {
        return Clock.parseMilliseconds(senderTimestampMilliseconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollUpdate that
                && Objects.equals(pollUpdateMessageKey, that.pollUpdateMessageKey)
                && Objects.equals(vote, that.vote)
                && senderTimestampMilliseconds == that.senderTimestampMilliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollUpdateMessageKey, vote, senderTimestampMilliseconds);
    }

    @Override
    public String toString() {
        return "PollUpdate[" +
                "pollUpdateMessageKey=" + pollUpdateMessageKey +
                ", vote=" + vote +
                ", senderTimestampMilliseconds=" + senderTimestampMilliseconds +
                ']';
    }
}