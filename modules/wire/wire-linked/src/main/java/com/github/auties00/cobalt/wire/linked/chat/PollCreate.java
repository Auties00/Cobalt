package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Input model for {@code WhatsAppClient.createPoll}. Carries the chat
 * the poll is being posted into, the poll question, the list of
 * options, and how many of those options a voter may select.
 *
 * <p>All four fields are required — the message body, the option list,
 * and the selectable-count fan out into the wire {@code PollCreationMessage}.
 */
@ProtobufMessage
public final class PollCreate {
    /**
     * JID of the chat the poll is being posted into.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid chat;

    /**
     * Poll question that is rendered as the message body.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String question;

    /**
     * Ordered list of selectable answer options.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> options;

    /**
     * Maximum number of options a single voter may select (1 for
     * single-choice, &gt;1 for multi-choice).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int selectableCount;

    /**
     * Constructs a new {@code PollCreate}.
     *
     * @param chat            the chat JID; required
     * @param question        the poll question; required
     * @param options         the answer options; required, non-empty
     * @param selectableCount the maximum number of selectable options
     * @throws NullPointerException if any reference argument is {@code null}
     */
    PollCreate(Jid chat, String question, List<String> options, int selectableCount) {
        this.chat = Objects.requireNonNull(chat, "chat cannot be null");
        this.question = Objects.requireNonNull(question, "question cannot be null");
        this.options = Objects.requireNonNull(options, "options cannot be null");
        this.selectableCount = selectableCount;
    }

    /**
     * Returns the chat JID.
     *
     * @return the chat JID, never {@code null}
     */
    public Jid chat() {
        return chat;
    }

    /**
     * Returns the poll question.
     *
     * @return the question, never {@code null}
     */
    public String question() {
        return question;
    }

    /**
     * Returns the answer options.
     *
     * @return the options, never {@code null}
     */
    public List<String> options() {
        return options;
    }

    /**
     * Returns the maximum number of options a voter may select.
     *
     * @return the selectable count
     */
    public int selectableCount() {
        return selectableCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PollCreate) obj;
        return Objects.equals(chat, that.chat) &&
                Objects.equals(question, that.question) &&
                Objects.equals(options, that.options) &&
                selectableCount == that.selectableCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chat, question, options, selectableCount);
    }

    @Override
    public String toString() {
        return "PollCreate[" +
                "chat=" + chat + ", " +
                "question=" + question + ", " +
                "options=" + options + ", " +
                "selectableCount=" + selectableCount + ']';
    }
}
