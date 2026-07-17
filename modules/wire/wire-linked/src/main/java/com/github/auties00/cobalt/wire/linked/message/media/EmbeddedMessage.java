package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A reference to an existing chat message that is being embedded inside another
 * piece of content.
 *
 * <p>This variant of {@link EmbeddedContent} is used when a message (for example a
 * link preview or status) wants to display another message inline. The embedded
 * reference carries both the stanza identifier of the original message and the
 * original {@link LinkedMessageContainer} so that clients can render it without refetching.
 */
@ProtobufMessage(name = "EmbeddedMessage")
public final class EmbeddedMessage implements EmbeddedContentVariant {
    /**
     * Stanza identifier of the original message being embedded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * Full message container of the original message being embedded.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainer;


    /**
     * Constructs a new embedded message reference.
     *
     * @param stanzaId         the stanza identifier of the referenced message
     * @param messageContainer the content of the referenced message
     */
    EmbeddedMessage(String stanzaId, LinkedMessageContainer messageContainer) {
        this.stanzaId = stanzaId;
        this.messageContainer = messageContainer;
    }

    /**
     * Returns the stanza identifier of the embedded message.
     *
     * @return the stanza id, or empty if unset
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Returns the message container of the embedded message.
     *
     * @return the embedded message content, or empty if not provided
     */
    public Optional<LinkedMessageContainer> message() {
        return Optional.ofNullable(messageContainer);
    }

    /**
     * Updates the stanza identifier of the embedded message.
     *
     * @param stanzaId the new stanza id, or {@code null} to clear
     */
    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    /**
     * Updates the embedded message container.
     *
     * @param messageContainer the new embedded message content, or {@code null} to clear
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }
}
