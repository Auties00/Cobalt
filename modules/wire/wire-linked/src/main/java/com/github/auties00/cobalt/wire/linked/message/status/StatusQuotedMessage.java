package com.github.auties00.cobalt.wire.linked.message.status;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Message that quotes an interactive element from a status, such as a
 * submitted question-answer, preserving a lightweight preview of the
 * original status inline with the reply.
 *
 * <p>Whereas {@link StatusMentionMessage} inlines the full source status
 * as a {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer},
 * this message carries only the minimal data needed to render a quote
 * surface: the interaction {@link StatusQuotedMessageType}, the quoted
 * text, an optional thumbnail of the source status, and the
 * {@link MessageKey} of the original status for linking back.
 *
 * @see MessageKey
 * @see StatusQuotedMessageType
 */
@ProtobufMessage(name = "Message.StatusQuotedMessage")
public final class StatusQuotedMessage implements Message {
    /**
     * Interaction kind that produced this quote.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    StatusQuotedMessageType type;

    /**
     * Quoted text rendered in the quote surface.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * Thumbnail preview of the original status, in JPEG bytes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] thumbnail;

    /**
     * Key of the original status being quoted.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    MessageKey originalStatusId;

    /**
     * Constructs a new {@code StatusQuotedMessage} with the supplied
     * fields.
     *
     * @param type             the interaction kind, or {@code null} if absent
     * @param text             the quoted text, or {@code null} if absent
     * @param thumbnail        the JPEG thumbnail bytes, or {@code null} if absent
     * @param originalStatusId the original status key, or {@code null} if absent
     */
    StatusQuotedMessage(StatusQuotedMessageType type, String text, byte[] thumbnail, MessageKey originalStatusId) {
        this.type = type;
        this.text = text;
        this.thumbnail = thumbnail;
        this.originalStatusId = originalStatusId;
    }

    /**
     * Returns the interaction kind that produced this quote.
     *
     * @return the interaction kind, or {@code Optional.empty()} if absent
     */
    public Optional<StatusQuotedMessageType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the quoted text rendered in the quote surface.
     *
     * @return the quoted text, or {@code Optional.empty()} if absent
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the thumbnail preview of the original status, in JPEG bytes.
     *
     * @return the thumbnail bytes, or {@code Optional.empty()} if absent
     */
    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    /**
     * Returns the key of the original status being quoted.
     *
     * @return the original status key, or {@code Optional.empty()} if absent
     */
    public Optional<MessageKey> originalStatusId() {
        return Optional.ofNullable(originalStatusId);
    }

    /**
     * Sets the interaction kind that produced this quote.
     *
     * @param type the interaction kind, or {@code null} to clear
     */
    public void setType(StatusQuotedMessageType type) {
        this.type = type;
    }

    /**
     * Sets the quoted text rendered in the quote surface.
     *
     * @param text the quoted text, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the thumbnail preview of the original status.
     *
     * @param thumbnail the JPEG thumbnail bytes, or {@code null} to clear
     */
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * Sets the key of the original status being quoted.
     *
     * @param originalStatusId the original status key, or {@code null} to clear
     */
    public void setOriginalStatusId(MessageKey originalStatusId) {
        this.originalStatusId = originalStatusId;
    }

    /**
     * Enumerates the interaction kinds that can produce a
     * {@link StatusQuotedMessage}.
     */
    @ProtobufEnum(name = "Message.StatusQuotedMessage.StatusQuotedMessageType")
    public static enum StatusQuotedMessageType {
        /**
         * Quote was produced by answering a question posted on the status.
         */
        QUESTION_ANSWER(1);

        /**
         * Constructs a new {@code StatusQuotedMessageType} enum constant.
         *
         * @param index the protobuf wire index
         */
        StatusQuotedMessageType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index used to serialise this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
