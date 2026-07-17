package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message that carries the textual answer to a question previously
 * posted in the chat.
 *
 * <p>It references the original prompt through its {@link MessageKey} and
 * stores the reply verbatim as plain text, enabling clients to surface the
 * exchange as a question-answer pair.
 */
@ProtobufMessage(name = "Message.QuestionResponseMessage")
public final class QuestionResponseMessage implements Message {
    /**
     * The {@link MessageKey} that identifies the question this message answers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The textual answer to the referenced question.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;


    /**
     * Constructs a new question response message.
     *
     * @param key  the key of the question being answered, may be {@code null}
     * @param text the textual answer, may be {@code null}
     */
    QuestionResponseMessage(MessageKey key, String text) {
        this.key = key;
        this.text = text;
    }

    /**
     * Returns the {@link MessageKey} of the question this message answers.
     *
     * @return an {@link Optional} containing the key, or
     *         {@link Optional#empty()} if no key is set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the textual answer to the referenced question.
     *
     * @return an {@link Optional} containing the text, or
     *         {@link Optional#empty()} if no text is set
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Sets the {@link MessageKey} of the question this message answers.
     *
     * @param key the new message key, or {@code null} to clear it
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the textual answer to the referenced question.
     *
     * @param text the new answer text, or {@code null} to clear it
     */
    public void setText(String text) {
        this.text = text;
    }
}
