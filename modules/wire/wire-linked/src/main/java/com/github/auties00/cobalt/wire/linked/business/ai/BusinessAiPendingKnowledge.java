package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A single piece of knowledge a WhatsApp Business AI agent inferred and is
 * proposing to its operator for approval.
 *
 * <p>As the WhatsApp Business AI agent (the merchant's auto-reply assistant)
 * answers chats, it can infer new facts about the business from those
 * conversations: a recurring question and its answer, or an attribute of the
 * business and its value. Rather than adopting inferred facts silently, the
 * assistant queues each one for the operator to keep or discard. This model
 * is one such queued item.
 *
 * <p>An item carries exactly one of two payload shapes, selected by its
 * {@link #type()}: a question/answer pair surfaced through
 * {@link #question()} and {@link #answer()}, or a business-attribute pair
 * surfaced through {@link #attributeName()} and {@link #attributeValue()}.
 * The {@link #id()} is the handle the operator passes back to approve the
 * item.
 */
@ProtobufMessage(name = "BusinessAiPendingKnowledge")
public final class BusinessAiPendingKnowledge {
    /**
     * Server-issued identifier of this proposed knowledge item. This is the
     * handle the operator passes back to approve the item; it is not a
     * WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Server-defined marker discriminating the item's payload shape (a
     * question/answer pair versus a business-attribute pair). The full value
     * set is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String type;

    /**
     * Inferred question text, present when the item proposes a
     * question/answer pair. Empty otherwise.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String question;

    /**
     * Inferred answer text, present when the item proposes a question/answer
     * pair. Empty otherwise.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String answer;

    /**
     * Inferred business-attribute name (for example a delivery policy or
     * opening hours), present when the item proposes a business-attribute
     * pair. Empty otherwise.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String attributeName;

    /**
     * Inferred business-attribute value, present when the item proposes a
     * business-attribute pair. Empty otherwise.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String attributeValue;

    /**
     * Constructs a new {@code BusinessAiPendingKnowledge}. Every argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param id             the proposed item identifier, or {@code null}
     * @param type           the payload-shape marker, or {@code null}
     * @param question       the inferred question text, or {@code null}
     * @param answer         the inferred answer text, or {@code null}
     * @param attributeName  the inferred attribute name, or {@code null}
     * @param attributeValue the inferred attribute value, or {@code null}
     */
    BusinessAiPendingKnowledge(String id, String type, String question, String answer, String attributeName, String attributeValue) {
        this.id = id;
        this.type = type;
        this.question = question;
        this.answer = answer;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    /**
     * Returns the server-issued identifier of this proposed knowledge item.
     *
     * @return the item id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the marker discriminating the item's payload shape.
     *
     * @return the payload-shape marker, or empty when the server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the inferred question text of a proposed question/answer pair.
     *
     * @return the question, or empty when the item carries no question
     */
    public Optional<String> question() {
        return Optional.ofNullable(question);
    }

    /**
     * Returns the inferred answer text of a proposed question/answer pair.
     *
     * @return the answer, or empty when the item carries no answer
     */
    public Optional<String> answer() {
        return Optional.ofNullable(answer);
    }

    /**
     * Returns the inferred business-attribute name of a proposed attribute
     * pair.
     *
     * @return the attribute name, or empty when the item carries no
     *         attribute
     */
    public Optional<String> attributeName() {
        return Optional.ofNullable(attributeName);
    }

    /**
     * Returns the inferred business-attribute value of a proposed attribute
     * pair.
     *
     * @return the attribute value, or empty when the item carries no
     *         attribute
     */
    public Optional<String> attributeValue() {
        return Optional.ofNullable(attributeValue);
    }
}
