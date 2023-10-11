package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessageName;
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
import it.auties.whatsapp.util.Validate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message holding a vote for a poll inside
 */
@ProtobufMessageName("Message.PollUpdateMessage")
public final class PollUpdateMessage implements Message, EncryptedMessage {
    private Jid voter;

    @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
    private final ChatMessageKey pollCreationMessageKey;

    private PollCreationMessage pollCreationMessage;

    private List<PollOption> votes;

    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    private PollUpdateEncryptedMetadata encryptedMetadata;

    @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
    private final PollUpdateMessageMetadata metadata;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    private final long senderTimestampMilliseconds;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PollUpdateMessage(ChatMessageKey pollCreationMessageKey, PollUpdateEncryptedMetadata encryptedMetadata, PollUpdateMessageMetadata metadata, long senderTimestampMilliseconds) {
        this.pollCreationMessageKey = pollCreationMessageKey;
        this.encryptedMetadata = encryptedMetadata;
        this.metadata = metadata;
        this.senderTimestampMilliseconds = senderTimestampMilliseconds;
        this.votes = new ArrayList<>();
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
        Validate.isTrue(poll.message()
                .type() == MessageType.POLL_CREATION, "Expected a poll, got %s".formatted(poll.message().type()));
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