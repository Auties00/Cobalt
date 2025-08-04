package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.EncryptedMessage;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedMetadata;
import it.auties.whatsapp.model.poll.PollUpdateMessageMetadata;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message holding a vote for a poll inside
 */
@ProtobufMessage(name = "Message.PollUpdateMessage")
public final class PollUpdateMessage implements Message, EncryptedMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey pollCreationMessageKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    PollUpdateEncryptedMetadata encryptedMetadata;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final PollUpdateMessageMetadata metadata;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final long senderTimestampMilliseconds;

    @ProtobufProperty(index = 999, type = ProtobufType.STRING)
    Jid voter;

    @ProtobufProperty(index = 1000, type = ProtobufType.MESSAGE)
    PollCreationMessage pollCreationMessage;

    @ProtobufProperty(index = 1001, type = ProtobufType.MESSAGE)
    List<PollOption> votes;

    PollUpdateMessage(ChatMessageKey pollCreationMessageKey, PollUpdateEncryptedMetadata encryptedMetadata, PollUpdateMessageMetadata metadata, long senderTimestampMilliseconds, Jid voter, PollCreationMessage pollCreationMessage, List<PollOption> votes) {
        this.pollCreationMessageKey = pollCreationMessageKey;
        this.encryptedMetadata = encryptedMetadata;
        this.metadata = metadata;
        this.senderTimestampMilliseconds = senderTimestampMilliseconds;
        this.voter = voter;
        this.pollCreationMessage = pollCreationMessage;
        this.votes = votes;
    }

    /**
     * Constructs a new builder to create a PollCreationMessage The newsletters can be later sent using
     * {@link Whatsapp#sendMessage(ChatMessageInfo)}
     *
     * @param poll  the non-null poll where the vote should be cast
     * @param votes the votes to cast: this list will override previous votes, so it can be empty or null if you want to revoke all votes
     * @return a non-null new message
     */
    @ProtobufBuilder(className = "PollUpdateMessageSimpleBuilder")
    static PollUpdateMessage simpleBuilder(ChatMessageInfo poll, List<PollOption> votes) {
        if (poll.message().type() != Type.POLL_CREATION) {
            throw new IllegalArgumentException("Expected a poll, got %s".formatted(poll.message().type()));
        }
        var result = new PollUpdateMessageBuilder()
                .pollCreationMessageKey(poll.key())
                .senderTimestampMilliseconds(Clock.nowMilliseconds())
                .build();
        result.setPollCreationMessage((PollCreationMessage) poll.message().content());
        result.setVotes(votes);
        return result;
    }

    public Optional<PollUpdateEncryptedMetadata> encryptedMetadata() {
        return Optional.ofNullable(encryptedMetadata);
    }

    public void setEncryptedMetadata(PollUpdateEncryptedMetadata encryptedMetadata) {
        this.encryptedMetadata = encryptedMetadata;
    }

    public Optional<Jid> voter() {
        return Optional.ofNullable(voter);
    }

    public void setVoter(Jid voter) {
        this.voter = voter;
    }

    public ChatMessageKey pollCreationMessageKey() {
        return pollCreationMessageKey;
    }

    public Optional<PollCreationMessage> pollCreationMessage() {
        return Optional.ofNullable(pollCreationMessage);
    }

    public void setPollCreationMessage(PollCreationMessage pollCreationMessage) {
        this.pollCreationMessage = pollCreationMessage;
    }

    public List<PollOption> votes() {
        return Collections.unmodifiableList(votes);
    }

    public void setVotes(List<PollOption> votes) {
        this.votes = votes;
    }

    public Optional<PollUpdateMessageMetadata> metadata() {
        return Optional.ofNullable(metadata);
    }

    public long senderTimestampMilliseconds() {
        return senderTimestampMilliseconds;
    }

    public Optional<ZonedDateTime> senderTimestamp() {
        return Clock.parseSeconds(senderTimestampMilliseconds);
    }


    public String secretName() {
        return "Poll Vote";
    }

    @Override
    public Type type() {
        return Type.POLL_UPDATE;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }
}