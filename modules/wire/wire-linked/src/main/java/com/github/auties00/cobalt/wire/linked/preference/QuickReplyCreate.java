package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Input model for {@code LinkedWhatsAppClient.createQuickReply}. Carries the
 * shortcut trigger, the message body, and an optional list of keywords.
 *
 * <p>{@link #shortcut} and {@link #message} are required; the relay
 * rejects the mutation without them. {@link #keywords} is optional —
 * unset is treated as an empty list.
 */
@ProtobufMessage
public final class QuickReplyCreate {
    /**
     * Slash-trigger the user types to expand the quick reply.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String shortcut;

    /**
     * Body of the message that replaces the trigger when expanded.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String message;

    /**
     * Optional list of keywords that also expand to the quick reply.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> keywords;

    /**
     * Constructs a new {@code QuickReplyCreate}.
     *
     * @param shortcut the slash-trigger; required
     * @param message  the message body; required
     * @param keywords the optional list of keywords, or {@code null}
     * @throws NullPointerException if {@code shortcut} or {@code message}
     *                              is {@code null}
     */
    QuickReplyCreate(String shortcut, String message, List<String> keywords) {
        this.shortcut = Objects.requireNonNull(shortcut, "shortcut cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.keywords = keywords;
    }

    /**
     * Returns the slash-trigger.
     *
     * @return the shortcut, never {@code null}
     */
    public String shortcut() {
        return shortcut;
    }

    /**
     * Returns the message body.
     *
     * @return the message, never {@code null}
     */
    public String message() {
        return message;
    }

    /**
     * Returns the trigger keywords. The returned list is never
     * {@code null}; an unset keyword list reads as an empty list.
     *
     * @return the keywords
     */
    public List<String> keywords() {
        return keywords == null ? List.of() : keywords;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QuickReplyCreate) obj;
        return Objects.equals(shortcut, that.shortcut) &&
                Objects.equals(message, that.message) &&
                Objects.equals(keywords, that.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortcut, message, keywords);
    }

    @Override
    public String toString() {
        return "QuickReplyCreate[" +
                "shortcut=" + shortcut + ", " +
                "message=" + message + ", " +
                "keywords=" + keywords + ']';
    }
}
