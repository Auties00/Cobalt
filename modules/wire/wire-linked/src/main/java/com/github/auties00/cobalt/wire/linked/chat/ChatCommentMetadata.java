package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Holds metadata about a comment thread attached to a WhatsApp message.
 *
 * <p>WhatsApp supports threaded comments on certain message types (for example,
 * channel posts in newsletters). Each comment references a parent message via
 * its {@link MessageKey} and tracks the total number of replies in that thread.
 *
 * <p>This class is a protobuf message (wire name {@code CommentMetadata}) carried
 * inside {@link ChatMessageInfo} to associate a message with its comment context.
 */
@ProtobufMessage(name = "CommentMetadata")
public final class ChatCommentMetadata {
    /**
     * The key of the parent message that this comment is replying to.
     * When present, this identifies the root message of the comment thread.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey commentParentKey;

    /**
     * The total number of replies in the comment thread. This count may
     * be provided by the server to allow clients to display summary
     * information without loading all individual replies.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer replyCount;

    /**
     * Constructs a new {@code ChatCommentMetadata} with the specified values.
     *
     * @param commentParentKey the key of the parent message, or {@code null}
     * @param replyCount       the number of replies in the thread, or {@code null}
     */
    ChatCommentMetadata(MessageKey commentParentKey, Integer replyCount) {
        this.commentParentKey = commentParentKey;
        this.replyCount = replyCount;
    }

    /**
     * Returns the key of the parent message this comment is attached to.
     *
     * @return an {@link Optional} containing the parent message key, or empty
     *         if no parent key is set
     */
    public Optional<MessageKey> commentParentKey() {
        return Optional.ofNullable(commentParentKey);
    }

    /**
     * Returns the number of replies in this comment thread.
     *
     * @return an {@link OptionalInt} containing the reply count, or empty if
     *         the count is not available
     */
    public OptionalInt replyCount() {
        return replyCount == null ? OptionalInt.empty() : OptionalInt.of(replyCount);
    }

    /**
     * Sets the key of the parent message for this comment.
     *
     * @param commentParentKey the parent message key, or {@code null} to clear
     */
    public void setCommentParentKey(MessageKey commentParentKey) {
        this.commentParentKey = commentParentKey;
    }

    /**
     * Sets the number of replies in this comment thread.
     *
     * @param replyCount the reply count, or {@code null} to clear
     */
    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }
}
