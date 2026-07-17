package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One message of a chat-history backup uploaded to a WhatsApp Business AI agent.
 *
 * <p>When a merchant seeds their auto-reply assistant from past conversations,
 * each conversation is uploaded as an ordered list of messages. This model is
 * one such message: it carries the {@link #author() author} that sent it, the
 * {@link #messageType() message type}, the {@link #text() text} body, and the
 * {@link #timestamp() timestamp} at which it was sent.
 */
@ProtobufMessage(name = "AiChatHistoryMessage")
public final class AiChatHistoryMessage {
    /**
     * Author that sent this message. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String author;

    /**
     * Server-defined discriminator of the message kind. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String messageType;

    /**
     * Text body of the message. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String text;

    /**
     * Timestamp at which the message was sent, carried as the client encodes
     * it on the wire. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String timestamp;

    /**
     * Constructs a new {@code AiChatHistoryMessage}. Every argument may be
     * {@code null} to leave the corresponding field unset.
     *
     * @param author      the message author, or {@code null}
     * @param messageType the message-type discriminator, or {@code null}
     * @param text        the text body, or {@code null}
     * @param timestamp   the send timestamp, or {@code null}
     */
    AiChatHistoryMessage(String author, String messageType, String text, String timestamp) {
        this.author = author;
        this.messageType = messageType;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     * Returns the author that sent this message.
     *
     * @return an {@link Optional} carrying the author, or empty when unset
     */
    public Optional<String> author() {
        return Optional.ofNullable(author);
    }

    /**
     * Returns the discriminator of the message kind.
     *
     * @return an {@link Optional} carrying the message type, or empty when unset
     */
    public Optional<String> messageType() {
        return Optional.ofNullable(messageType);
    }

    /**
     * Returns the text body of the message.
     *
     * @return an {@link Optional} carrying the text, or empty when unset
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the timestamp at which the message was sent.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when unset
     */
    public Optional<String> timestamp() {
        return Optional.ofNullable(timestamp);
    }
}
