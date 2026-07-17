package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Represents a vote cast by a participant against an existing poll.
 *
 * <p>When a user selects one or more options in a poll, the client sends a
 * poll update message back to the poll creator. The message references the
 * original {@link PollCreationMessage} by {@link MessageKey} and carries the
 * encrypted list of chosen options as a {@link PollEncValue}. Additional
 * metadata and the sender's timestamp help reconstruct the vote history.
 */
@ProtobufMessage(name = "Message.PollUpdateMessage")
public final class PollUpdateMessage implements Message {
    /**
     * The key of the {@link PollCreationMessage} this vote refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey pollCreationMessageKey;

    /**
     * The encrypted list of options selected by the voter.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    PollEncValue vote;

    /**
     * Additional metadata associated with this vote.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    PollUpdateMessageMetadata metadata;

    /**
     * The instant at which the voter cast this vote according to the sender.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;

    /**
     * The raw option labels selected by the voter, used only as send-time input.
     *
     * <p>This field is deliberately not a {@link ProtobufProperty}: it never
     * travels on the wire. When a poll vote built from raw options is dispatched
     * through the send pipeline, the addon-preparation step resolves the
     * referenced {@link PollCreationMessage} and encrypts these labels into
     * {@link #vote}. It is {@code null} on an already-encrypted or received vote.
     */
    List<String> selectedOptions;


    /**
     * Creates a new poll update message with the provided values.
     *
     * @param pollCreationMessageKey the key of the originating poll creation message
     * @param vote                   the encrypted selected options
     * @param metadata               additional vote metadata
     * @param senderTimestampMs      the instant at which the vote was cast
     */
    PollUpdateMessage(MessageKey pollCreationMessageKey, PollEncValue vote, PollUpdateMessageMetadata metadata, Instant senderTimestampMs) {
        this.pollCreationMessageKey = pollCreationMessageKey;
        this.vote = vote;
        this.metadata = metadata;
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Returns the key of the {@link PollCreationMessage} this vote refers to.
     *
     * @return an {@link Optional} containing the {@link MessageKey}, or empty when absent
     */
    public Optional<MessageKey> pollCreationMessageKey() {
        return Optional.ofNullable(pollCreationMessageKey);
    }

    /**
     * Returns the encrypted list of options selected by the voter.
     *
     * @return an {@link Optional} containing the {@link PollEncValue}, or empty when absent
     */
    public Optional<PollEncValue> vote() {
        return Optional.ofNullable(vote);
    }

    /**
     * Returns the additional metadata associated with this vote.
     *
     * @return an {@link Optional} containing the {@link PollUpdateMessageMetadata}, or empty when absent
     */
    public Optional<PollUpdateMessageMetadata> metadata() {
        return Optional.ofNullable(metadata);
    }

    /**
     * Returns the instant at which the voter cast this vote according to the sender.
     *
     * @return an {@link Optional} containing the sender's {@link Instant}, or empty when absent
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Sets the key of the {@link PollCreationMessage} this vote refers to.
     *
     * @param pollCreationMessageKey the {@link MessageKey} of the poll creation message
     */
    public void setPollCreationMessageKey(MessageKey pollCreationMessageKey) {
        this.pollCreationMessageKey = pollCreationMessageKey;
    }

    /**
     * Sets the encrypted list of options selected by the voter.
     *
     * @param vote the {@link PollEncValue} carrying the encrypted vote
     */
    public void setVote(PollEncValue vote) {
        this.vote = vote;
    }

    /**
     * Sets the additional metadata associated with this vote.
     *
     * @param metadata the {@link PollUpdateMessageMetadata}
     */
    public void setMetadata(PollUpdateMessageMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Sets the instant at which the voter cast this vote according to the sender.
     *
     * @param senderTimestampMs the sender's {@link Instant}
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Returns the raw option labels selected by the voter for send-time encryption.
     *
     * @return an unmodifiable list of selected option labels; never {@code null},
     *         and empty once the vote has been encrypted or on a received vote
     */
    public List<String> selectedOptions() {
        return selectedOptions == null ? List.of() : selectedOptions;
    }

    /**
     * Sets the raw option labels selected by the voter.
     *
     * <p>Supplying these defers vote encryption to the send pipeline, which
     * resolves the {@link PollCreationMessage} and populates {@link #vote}; the
     * labels themselves are never serialized.
     *
     * @param selectedOptions the selected option labels, or {@code null} to clear
     */
    public void setSelectedOptions(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }
}
