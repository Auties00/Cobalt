package com.github.auties00.cobalt.wire.linked.business.ai;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * One ordered piece of knowledge a WhatsApp Business AI agent answers from.
 *
 * <p>The merchant's auto-reply assistant draws on an ordered list of
 * knowledge entries. An entry holds one of two payload shapes, selected by
 * {@link #dataType()}: a free-text statement surfaced through
 * {@link #text()}, or a structured question/answer pair surfaced through
 * {@link #exampleResponse()}. The {@link #knowledgeType()} marker records
 * the broader category the entry belongs to, and {@link #lastUpdated()}
 * records when it last changed.
 */
@ProtobufMessage(name = "BusinessAiKnowledgeEntry")
public final class BusinessAiKnowledgeEntry {
    /**
     * Server-defined marker recording the broad category the entry belongs
     * to. The full value set is not recoverable from the WhatsApp client, so
     * the raw marker is exposed as a string. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String knowledgeType;

    /**
     * Server-defined marker selecting the entry's payload shape (free text
     * versus a question/answer pair). The full value set is not recoverable
     * from the WhatsApp client, so the raw marker is exposed as a string.
     * Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String dataType;

    /**
     * Free-text knowledge statement, present when the entry is a free-text
     * entry. Empty otherwise.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String text;

    /**
     * Structured question/answer knowledge, present when the entry is a
     * frequently-asked-question entry. Empty otherwise.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final BusinessAiExampleResponse exampleResponse;

    /**
     * Instant the entry was last updated. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant lastUpdated;

    /**
     * Constructs a new {@code BusinessAiKnowledgeEntry}. Every argument may
     * be {@code null} when the server omitted the corresponding field.
     *
     * @param knowledgeType   the category marker, or {@code null}
     * @param dataType        the payload-shape marker, or {@code null}
     * @param text            the free-text statement, or {@code null}
     * @param exampleResponse the question/answer knowledge, or {@code null}
     * @param lastUpdated     the last-update instant, or {@code null}
     */
    BusinessAiKnowledgeEntry(String knowledgeType, String dataType, String text, BusinessAiExampleResponse exampleResponse, Instant lastUpdated) {
        this.knowledgeType = knowledgeType;
        this.dataType = dataType;
        this.text = text;
        this.exampleResponse = exampleResponse;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Returns the marker recording the broad category the entry belongs to.
     *
     * @return the category marker, or empty when the server omitted it
     */
    public Optional<String> knowledgeType() {
        return Optional.ofNullable(knowledgeType);
    }

    /**
     * Returns the marker selecting the entry's payload shape.
     *
     * @return the payload-shape marker, or empty when the server omitted it
     */
    public Optional<String> dataType() {
        return Optional.ofNullable(dataType);
    }

    /**
     * Returns the free-text knowledge statement of a free-text entry.
     *
     * @return the free-text statement, or empty when the entry carries none
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the structured question/answer knowledge of an FAQ entry.
     *
     * @return the question/answer knowledge, or empty when the entry carries
     *         none
     */
    public Optional<BusinessAiExampleResponse> exampleResponse() {
        return Optional.ofNullable(exampleResponse);
    }

    /**
     * Returns the instant the entry was last updated.
     *
     * @return the last-update instant, or empty when the server omitted it
     */
    public Optional<Instant> lastUpdated() {
        return Optional.ofNullable(lastUpdated);
    }
}
