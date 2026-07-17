package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Represents a business quick reply template that lets the user expand a short
 * shortcut into a full canned message.
 *
 * <p>Quick replies are a WhatsApp Business feature: the user defines a
 * shortcut (for example {@code "/hello"}) together with the message body that
 * should be sent in its place (for example {@code "Hello, thanks for
 * contacting us. How can we help you today?"}). When the user types the
 * shortcut in the compose box, the client offers the template as a suggestion
 * and substitutes the full message on selection.
 *
 * <p>Each quick reply carries a stable identifier that acts as a primary key
 * in Cobalt's store, the shortcut text the user types, the message body that
 * is sent in its place, a list of search keywords that help surface the
 * template in autocomplete, and a usage counter that the client maintains so
 * that frequently used replies can be prioritised.
 *
 * <p>Instances are immutable. Use the generated {@code QuickReplyBuilder} to
 * create new quick replies.
 */
@ProtobufMessage
public final class QuickReply {
    /**
     * The stable identifier of this quick reply.
     *
     * <p>The identifier is used by Cobalt's store to key quick replies. Edits
     * that change the shortcut or message body while preserving the identifier
     * are treated as updates to the same entry.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String id;

    /**
     * The shortcut text the user types in the compose box to trigger this
     * quick reply.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String shortcut;

    /**
     * The full message body that is sent in place of the shortcut.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String message;

    /**
     * The list of search keywords associated with this quick reply.
     *
     * <p>Keywords help the autocomplete surface the template even when the
     * user does not type the shortcut verbatim.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> keywords;

    /**
     * The usage counter maintained by the client.
     *
     * <p>Clients increment this value each time the quick reply is expanded so
     * that frequently used templates can be prioritised in suggestions.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int count;

    /**
     * Constructs a new quick reply with the given field values.
     *
     * <p>This constructor is package-private. Application code should obtain
     * instances through the generated {@code QuickReplyBuilder}.
     *
     * @param id       the stable identifier, must not be {@code null}
     * @param shortcut the shortcut text the user types
     * @param message  the message body to send in place of the shortcut
     * @param keywords the search keywords
     * @param count    the usage counter
     * @throws NullPointerException if {@code id} is {@code null}
     */
    QuickReply(String id, String shortcut, String message, List<String> keywords, int count) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.shortcut = shortcut;
        this.message = message;
        this.keywords = keywords;
        this.count = count;
    }

    /**
     * Returns the stable identifier of this quick reply.
     *
     * @return the identifier, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the shortcut text the user types to trigger this quick reply.
     *
     * @return the shortcut text
     */
    public String shortcut() {
        return shortcut;
    }

    /**
     * Returns the full message body that is sent in place of the shortcut.
     *
     * @return the message body
     */
    public String message() {
        return message;
    }

    /**
     * Returns the search keywords associated with this quick reply.
     *
     * @return the keyword list
     */
    public List<String> keywords() {
        return keywords;
    }

    /**
     * Returns the number of times this quick reply has been used.
     *
     * @return the usage counter
     */
    public int count() {
        return count;
    }
}
