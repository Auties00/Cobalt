package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One frequently-asked-question entry a WhatsApp Business AI agent answers
 * from.
 *
 * <p>An example response teaches the merchant's auto-reply assistant a
 * canonical answer to a recurring question: when a customer asks something
 * matching {@link #question()}, the assistant replies with {@link #answer()}.
 * These entries form the assistant's editable frequently-asked-question
 * knowledge.
 */
@ProtobufMessage(name = "BusinessAiExampleResponse")
public final class BusinessAiExampleResponse {
    /**
     * Server-issued identifier of this question/answer entry. This is the
     * handle used to update or remove the entry; it is not a WhatsApp
     * address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Question the entry answers, as the merchant phrased it. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String question;

    /**
     * Canonical answer the assistant gives for the question. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String answer;

    /**
     * Server-defined marker classifying the question/answer entry (for
     * example merchant-authored versus assistant-suggested). The full value
     * set is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String entryType;

    /**
     * Constructs a new {@code BusinessAiExampleResponse}. Every argument may
     * be {@code null} when the server omitted the corresponding field.
     *
     * @param id        the entry identifier, or {@code null}
     * @param question  the question text, or {@code null}
     * @param answer    the answer text, or {@code null}
     * @param entryType the entry-classification marker, or {@code null}
     */
    BusinessAiExampleResponse(String id, String question, String answer, String entryType) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.entryType = entryType;
    }

    /**
     * Returns the server-issued identifier of this entry.
     *
     * @return the entry id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the question the entry answers.
     *
     * @return the question, or empty when the server omitted it
     */
    public Optional<String> question() {
        return Optional.ofNullable(question);
    }

    /**
     * Returns the canonical answer the assistant gives for the question.
     *
     * @return the answer, or empty when the server omitted it
     */
    public Optional<String> answer() {
        return Optional.ofNullable(answer);
    }

    /**
     * Returns the marker classifying the question/answer entry.
     *
     * @return the entry-classification marker, or empty when the server
     *         omitted it
     */
    public Optional<String> entryType() {
        return Optional.ofNullable(entryType);
    }
}
