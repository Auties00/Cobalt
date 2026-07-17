package com.github.auties00.cobalt.wire.linked.message.poll;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a message that creates a new poll inside a chat.
 *
 * <p>A poll creation message carries the poll question, the list of available
 * options, an encryption key used to protect individual votes, and metadata
 * such as the number of answers each voter may select and whether the poll is
 * a quiz. Votes sent in response to this poll are transmitted as
 * {@link PollUpdateMessage} messages referencing the key of this creation
 * message.
 *
 * <p>The {@code encKey} field holds the symmetric key that recipients use to
 * decrypt incoming {@link PollEncValue} payloads. When the poll is a quiz the
 * {@code correctAnswer} field identifies the option that voters need to pick
 * to answer correctly.
 */
@ProtobufMessage(name = "Message.PollCreationMessage")
public final class PollCreationMessage implements ContextualMessage {
    /**
     * The symmetric key used to encrypt votes cast against this poll.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encKey;

    /**
     * The question or title shown to voters.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The list of selectable options offered to voters.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<Option> options;

    /**
     * The maximum number of options each voter may select.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer selectableOptionsCount;

    /**
     * Contextual metadata such as quoted messages, mentions and forwarding info.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Describes whether options are rendered as text or images.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    PollContentType pollContentType;

    /**
     * Describes whether the poll behaves as a standard poll or a quiz.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    PollType pollType;

    /**
     * When the poll is a quiz, the option that counts as the correct answer.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    Option correctAnswer;


    /**
     * Creates a new poll creation message with the provided values.
     *
     * @param encKey                 the symmetric key used to encrypt votes
     * @param name                   the poll question or title
     * @param options                the list of available options
     * @param selectableOptionsCount the maximum number of options a voter may select
     * @param contextInfo            contextual metadata for the message
     * @param pollContentType        the content type of the options
     * @param pollType               whether this poll is a standard poll or a quiz
     * @param correctAnswer          the correct option when the poll is a quiz
     */
    PollCreationMessage(byte[] encKey, String name, List<Option> options, Integer selectableOptionsCount, ContextInfo contextInfo, PollContentType pollContentType, PollType pollType, Option correctAnswer) {
        this.encKey = encKey;
        this.name = name;
        this.options = options;
        this.selectableOptionsCount = selectableOptionsCount;
        this.contextInfo = contextInfo;
        this.pollContentType = pollContentType;
        this.pollType = pollType;
        this.correctAnswer = correctAnswer;
    }

    /**
     * Returns the symmetric key used to encrypt votes cast against this poll.
     *
     * @return an {@link Optional} containing the encryption key, or empty when absent
     */
    public Optional<byte[]> encKey() {
        return Optional.ofNullable(encKey);
    }

    /**
     * Returns the question or title of the poll.
     *
     * @return an {@link Optional} containing the poll name, or empty when absent
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the list of available options offered to voters.
     *
     * @return an unmodifiable list of options, or an empty list when none are set
     */
    public List<Option> options() {
        return options == null ? List.of() : Collections.unmodifiableList(options);
    }

    /**
     * Returns the maximum number of options each voter may select.
     *
     * @return an {@link OptionalInt} with the limit, or empty when unspecified
     */
    public OptionalInt selectableOptionsCount() {
        return selectableOptionsCount == null ? OptionalInt.empty() : OptionalInt.of(selectableOptionsCount);
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
     * Returns whether poll options are rendered as text or as images.
     *
     * @return an {@link Optional} containing the {@link PollContentType}, or empty when absent
     */
    public Optional<PollContentType> pollContentType() {
        return Optional.ofNullable(pollContentType);
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
     * Returns the option marked as the correct answer when this poll is a quiz.
     *
     * @return an {@link Optional} containing the correct {@link Option}, or empty when not a quiz
     */
    public Optional<Option> correctAnswer() {
        return Optional.ofNullable(correctAnswer);
    }

    /**
     * Sets the symmetric key used to encrypt votes cast against this poll.
     *
     * @param encKey the encryption key
     */
    public void setEncKey(byte[] encKey) {
        this.encKey = encKey;
    }

    /**
     * Sets the question or title of the poll.
     *
     * @param name the poll name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the list of available options offered to voters.
     *
     * @param options the list of options
     */
    public void setOptions(List<Option> options) {
        this.options = options;
    }

    /**
     * Sets the maximum number of options each voter may select.
     *
     * @param selectableOptionsCount the maximum selectable options count
     */
    public void setSelectableOptionsCount(Integer selectableOptionsCount) {
        this.selectableOptionsCount = selectableOptionsCount;
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
     * Sets whether poll options are rendered as text or as images.
     *
     * @param pollContentType the {@link PollContentType}
     */
    public void setPollContentType(PollContentType pollContentType) {
        this.pollContentType = pollContentType;
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
     * Sets the option marked as the correct answer when this poll is a quiz.
     *
     * @param correctAnswer the correct {@link Option}
     */
    public void setCorrectAnswer(Option correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    /**
     * Represents a single selectable option inside a {@link PollCreationMessage}.
     *
     * <p>Each option carries the text displayed to voters and a hash that
     * uniquely identifies it. The hash is used by vote messages to reference
     * the option without disclosing the label, preserving the confidentiality
     * of the ballot while still allowing the sender to tally results.
     */
    @ProtobufMessage(name = "Message.PollCreationMessage.Option")
    public static final class Option {
        /**
         * The label presented to voters for this option.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String optionName;

        /**
         * A hash identifying this option in vote messages.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String optionHash;


        /**
         * Creates a new option with the provided label and hash.
         *
         * @param optionName the label presented to voters
         * @param optionHash the hash identifying this option
         */
        Option(String optionName, String optionHash) {
            this.optionName = optionName;
            this.optionHash = optionHash;
        }

        /**
         * Returns the label presented to voters for this option.
         *
         * @return an {@link Optional} containing the option label, or empty when absent
         */
        public Optional<String> optionName() {
            return Optional.ofNullable(optionName);
        }

        /**
         * Returns the hash that identifies this option in vote messages.
         *
         * @return an {@link Optional} containing the hash, or empty when absent
         */
        public Optional<String> optionHash() {
            return Optional.ofNullable(optionHash);
        }

        /**
         * Sets the label presented to voters for this option.
         *
         * @param optionName the option label
         */
        public void setOptionName(String optionName) {
            this.optionName = optionName;
    }

        /**
         * Sets the hash that identifies this option in vote messages.
         *
         * @param optionHash the option hash
         */
        public void setOptionHash(String optionHash) {
            this.optionHash = optionHash;
    }
    }
}
