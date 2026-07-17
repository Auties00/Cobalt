package com.github.auties00.cobalt.wire.linked.message.status;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Reply sent by a viewer to a question posted on someone's status.
 *
 * <p>WhatsApp statuses can include an interactive "question" sticker that
 * prompts viewers to type a free-form answer. When a viewer submits an
 * answer, the client sends back this message: it links to the original
 * status via {@link #key()} and carries the viewer's typed response in
 * {@link #text()}.
 *
 * @see MessageKey
 */
@ProtobufMessage(name = "Message.StatusQuestionAnswerMessage")
public final class StatusQuestionAnswerMessage implements Message {
    /**
     * Key of the original status that carried the question.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * Text answer supplied by the viewer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * Constructs a new {@code StatusQuestionAnswerMessage} with the
     * supplied fields.
     *
     * @param key  the original status key, or {@code null} if absent
     * @param text the answer text, or {@code null} if absent
     */
    StatusQuestionAnswerMessage(MessageKey key, String text) {
        this.key = key;
        this.text = text;
    }

    /**
     * Returns the key of the original status that carried the question.
     *
     * @return the original status key, or {@code Optional.empty()} if absent
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the text answer supplied by the viewer.
     *
     * @return the answer text, or {@code Optional.empty()} if absent
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Sets the key of the original status that carried the question.
     *
     * @param key the original status key, or {@code null} to clear
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the text answer supplied by the viewer.
     *
     * @param text the answer text, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }
}
