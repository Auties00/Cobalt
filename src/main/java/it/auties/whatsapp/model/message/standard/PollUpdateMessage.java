package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.*;
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
    private final ChatMessageKey pollCreationMessageKey;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private PollUpdateEncryptedMetadata encryptedMetadata;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final PollUpdateMessageMetadata metadata;
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    private final long senderTimestampMilliseconds;
    @ProtobufProperty(index = 999, type = ProtobufType.STRING)
    private Jid voter;
    @ProtobufProperty(index = 1000, type = ProtobufType.MESSAGE)
    private PollCreationMessage pollCreationMessage;
    @ProtobufProperty(index = 1001, type = ProtobufType.MESSAGE)
    private List<PollOption> votes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PollUpdateMessage(ChatMessageKey pollCreationMessageKey, PollUpdateEncryptedMetadata encryptedMetadata, PollUpdateMessageMetadata metadata, long senderTimestampMilliseconds, Jid voter, PollCreationMessage pollCreationMessage, List<PollOption> votes) {
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
        if (poll.message().type() != MessageType.POLL_CREATION) {
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

    public PollUpdateMessage setEncryptedMetadata(PollUpdateEncryptedMetadata encryptedMetadata) {
        this.encryptedMetadata = encryptedMetadata;
        return this;
    }

    public Optional<Jid> voter() {
        return Optional.ofNullable(voter);
    }

    public PollUpdateMessage setVoter(Jid voter) {
        this.voter = voter;
        return this;
    }

    public ChatMessageKey pollCreationMessageKey() {
        return pollCreationMessageKey;
    }

    public Optional<PollCreationMessage> pollCreationMessage() {
        return Optional.ofNullable(pollCreationMessage);
    }

    public PollUpdateMessage setPollCreationMessage(PollCreationMessage pollCreationMessage) {
        this.pollCreationMessage = pollCreationMessage;
        return this;
    }

    public List<PollOption> votes() {
        return Collections.unmodifiableList(votes);
    }

    public PollUpdateMessage setVotes(List<PollOption> votes) {
        this.votes = votes;
        return this;
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
    public MessageType type() {
        return MessageType.POLL_UPDATE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}