package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Input model for {@code LinkedWhatsAppClient.editQuickReply}. Carries the id
 * of the quick reply being edited together with its new shortcut,
 * message body, and optional keyword list.
 *
 * <p>{@link #id}, {@link #shortcut} and {@link #message} are required;
 * the relay rejects the mutation without them. {@link #keywords} is
 * optional — unset is treated as an empty list.
 */
@ProtobufMessage
public final class QuickReplyEdit {
    /**
     * Identifier of the existing quick reply being edited.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * New slash-trigger for the quick reply.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String shortcut;

    /**
     * New message body for the quick reply.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String message;

    /**
     * Optional new keyword list. {@code null} preserves the existing
     * keywords on the server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> keywords;

    /**
     * Constructs a new {@code QuickReplyEdit}.
     *
     * @param id       the existing quick-reply identifier; required
     * @param shortcut the new slash-trigger; required
     * @param message  the new message body; required
     * @param keywords the optional new keyword list, or {@code null}
     * @throws NullPointerException if {@code id}, {@code shortcut} or
     *                              {@code message} is {@code null}
     */
    QuickReplyEdit(String id, String shortcut, String message, List<String> keywords) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.shortcut = Objects.requireNonNull(shortcut, "shortcut cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.keywords = keywords;
    }

    /**
     * Returns the existing quick-reply identifier.
     *
     * @return the id, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the new slash-trigger.
     *
     * @return the shortcut, never {@code null}
     */
    public String shortcut() {
        return shortcut;
    }

    /**
     * Returns the new message body.
     *
     * @return the message, never {@code null}
     */
    public String message() {
        return message;
    }

    /**
     * Returns the new keyword list. The returned list is never
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
        var that = (QuickReplyEdit) obj;
        return Objects.equals(id, that.id) &&
                Objects.equals(shortcut, that.shortcut) &&
                Objects.equals(message, that.message) &&
                Objects.equals(keywords, that.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortcut, message, keywords);
    }

    @Override
    public String toString() {
        return "QuickReplyEdit[" +
                "id=" + id + ", " +
                "shortcut=" + shortcut + ", " +
                "message=" + message + ", " +
                "keywords=" + keywords + ']';
    }
}
