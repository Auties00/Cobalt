package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a snapshot of the current tally of a poll.
 *
 * <p>The poll creator produces this message to broadcast the current vote
 * counts to participants. Unlike individual {@link PollUpdateMessage} votes,
 * which are encrypted and only readable by the creator, a result snapshot
 * exposes the aggregated counts in plain text so that every participant can
 * see the running total.
 *
 * <p>The snapshot includes the poll question, the list of per-option vote
 * totals, the poll type (standard or quiz) and contextual metadata.
 */
@ProtobufMessage(name = "Message.PollResultSnapshotMessage")
public final class PollResultSnapshotMessage implements ContextualMessage {
    /**
     * The question or title of the poll this snapshot refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * The current per-option vote totals.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<PollVote> pollVotes;

    /**
     * Contextual metadata such as quoted messages, mentions and forwarding info.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Describes whether the poll behaves as a standard poll or a quiz.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    PollType pollType;


    /**
     * Creates a new poll result snapshot with the provided values.
     *
     * @param name        the poll question or title
     * @param pollVotes   the per-option vote totals
     * @param contextInfo contextual metadata for the message
     * @param pollType    whether this poll is a standard poll or a quiz
     */
    PollResultSnapshotMessage(String name, List<PollVote> pollVotes, ContextInfo contextInfo, PollType pollType) {
        this.name = name;
        this.pollVotes = pollVotes;
        this.contextInfo = contextInfo;
        this.pollType = pollType;
    }

    /**
     * Returns the question or title of the poll this snapshot refers to.
     *
     * @return an {@link Optional} containing the poll name, or empty when absent
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the current per-option vote totals.
     *
     * @return an unmodifiable list of {@link PollVote}, or an empty list when none are set
     */
    public List<PollVote> pollVotes() {
        return pollVotes == null ? List.of() : Collections.unmodifiableList(pollVotes);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the {@link ContextInfo}, or empty when absent
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns whether this poll behaves as a standard poll or a quiz.
     *
     * @return an {@link Optional} containing the {@link PollType}, or empty when absent
     */
    public Optional<PollType> pollType() {
        return Optional.ofNullable(pollType);
    }

    /**
     * Sets the question or title of the poll this snapshot refers to.
     *
     * @param name the poll name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the current per-option vote totals.
     *
     * @param pollVotes the per-option vote totals
     */
    public void setPollVotes(List<PollVote> pollVotes) {
        this.pollVotes = pollVotes;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the {@link ContextInfo}
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets whether this poll behaves as a standard poll or a quiz.
     *
     * @param pollType the {@link PollType}
     */
    public void setPollType(PollType pollType) {
        this.pollType = pollType;
    }

    /**
     * Represents the tally for a single option in a {@link PollResultSnapshotMessage}.
     *
     * <p>Each entry pairs the plain-text label of a poll option with the number
     * of votes it has received so far.
     */
    @ProtobufMessage(name = "Message.PollResultSnapshotMessage.PollVote")
    public static final class PollVote {
        /**
         * The label of the option this tally refers to.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String optionName;

        /**
         * The number of votes this option has received.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        Long optionVoteCount;


        /**
         * Creates a new option tally with the provided label and count.
         *
         * @param optionName      the label of the option
         * @param optionVoteCount the number of votes for the option
         */
        PollVote(String optionName, Long optionVoteCount) {
            this.optionName = optionName;
            this.optionVoteCount = optionVoteCount;
        }

        /**
         * Returns the label of the option this tally refers to.
         *
         * @return an {@link Optional} containing the option label, or empty when absent
         */
        public Optional<String> optionName() {
            return Optional.ofNullable(optionName);
        }

        /**
         * Returns the number of votes this option has received.
         *
         * @return an {@link OptionalLong} with the vote count, or empty when absent
         */
        public OptionalLong optionVoteCount() {
            return optionVoteCount == null ? OptionalLong.empty() : OptionalLong.of(optionVoteCount);
        }

        /**
         * Sets the label of the option this tally refers to.
         *
         * @param optionName the option label
         */
        public void setOptionName(String optionName) {
            this.optionName = optionName;
    }

        /**
         * Sets the number of votes this option has received.
         *
         * @param optionVoteCount the vote count
         */
        public void setOptionVoteCount(Long optionVoteCount) {
            this.optionVoteCount = optionVoteCount;
    }
    }
}
