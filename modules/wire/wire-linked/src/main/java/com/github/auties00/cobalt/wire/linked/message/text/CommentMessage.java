package com.github.auties00.cobalt.wire.linked.message.text;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that represents a comment posted on another message within a
 * WhatsApp channel or community thread.
 *
 * <p>Comment messages wrap an inner {@link LinkedMessageContainer} carrying the
 * actual comment content, together with a {@link MessageKey} identifying
 * the parent message the comment is attached to. Clients use this type to
 * render threaded replies beneath broadcast posts and to navigate from a
 * comment back to the post it refers to.
 */
@ProtobufMessage(name = "Message.CommentMessage")
public final class CommentMessage implements Message {
    /**
     * The wrapped comment payload containing the actual content the user
     * posted in reply to the parent message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainer;

    /**
     * The key identifying the message this comment is attached to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageKey targetMessageKey;


    /**
     * Constructs a new comment message with the supplied payload and target.
     *
     * @param messageContainer the wrapped comment content
     * @param targetMessageKey the key of the message this comment replies to
     */
    CommentMessage(LinkedMessageContainer messageContainer, MessageKey targetMessageKey) {
        this.messageContainer = messageContainer;
        this.targetMessageKey = targetMessageKey;
    }

    /**
     * Returns the wrapped comment payload, if present.
     *
     * @return an {@link Optional} containing the inner {@code LinkedMessageContainer},
     *         or {@link Optional#empty()} if the payload is absent
     */
    public Optional<LinkedMessageContainer> message() {
        return Optional.ofNullable(messageContainer);
    }

    /**
     * Returns the key of the message this comment is attached to, if present.
     *
     * @return an {@link Optional} containing the target {@code MessageKey},
     *         or {@link Optional#empty()} if no target has been set
     */
    public Optional<MessageKey> targetMessageKey() {
        return Optional.ofNullable(targetMessageKey);
    }

    /**
     * Sets the wrapped comment payload.
     *
     * @param messageContainer the comment content, or {@code null} to clear
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    /**
     * Sets the key of the message this comment is attached to.
     *
     * @param targetMessageKey the target key, or {@code null} to clear
     */
    public void setTargetMessageKey(MessageKey targetMessageKey) {
        this.targetMessageKey = targetMessageKey;
    }
}
