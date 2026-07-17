package com.github.auties00.cobalt.wire.linked.message;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Identifies a sub-thread a message belongs to within a chat.
 *
 * <p>WhatsApp supports threaded conversations in a handful of
 * scenarios, most notably the "view replies" flow where replies to a
 * specific message are grouped together, and AI chats where the
 * conversation with the bot is segmented into individual sessions. A
 * {@code MessageThreadId} pairs the key of the message that anchors
 * the thread with the type of thread it represents, letting clients
 * associate a message with the right sub-thread when rendering the
 * conversation.
 */
@ProtobufMessage(name = "ThreadID")
public final class MessageThreadId {
    /**
     * The kind of thread this identifier points to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    ThreadType threadType;

    /**
     * The key of the message that anchors the thread.
     *
     * <p>For view-replies threads this is the key of the parent
     * message being replied to; for AI threads it is the key of the
     * message that started the AI session.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageKey threadKey;


    /**
     * Constructs a new {@code MessageThreadId}.
     *
     * <p>The constructor is package-private; use
     * {@code MessageThreadIdBuilder} to instantiate new values.
     *
     * @param threadType the type of thread
     * @param threadKey  the key of the anchoring message
     */
    MessageThreadId(ThreadType threadType, MessageKey threadKey) {
        this.threadType = threadType;
        this.threadKey = threadKey;
    }

    /**
     * Returns the kind of thread this identifier points to.
     *
     * @return an {@link Optional} holding the {@link ThreadType}, or
     *         empty if none was set
     */
    public Optional<ThreadType> threadType() {
        return Optional.ofNullable(threadType);
    }

    /**
     * Returns the key of the message that anchors the thread.
     *
     * @return an {@link Optional} holding the anchor key, or empty if
     *         none was set
     */
    public Optional<MessageKey> threadKey() {
        return Optional.ofNullable(threadKey);
    }

    /**
     * Updates the kind of thread.
     *
     * @param threadType the new thread type, or {@code null} to clear
     */
    public void setThreadType(ThreadType threadType) {
        this.threadType = threadType;
    }

    /**
     * Updates the key of the message that anchors the thread.
     *
     * @param threadKey the new anchor key, or {@code null} to clear
     */
    public void setThreadKey(MessageKey threadKey) {
        this.threadKey = threadKey;
    }

    /**
     * Enumerates the kinds of threaded conversations supported by
     * WhatsApp.
     */
    @ProtobufEnum(name = "ThreadID.ThreadType")
    public static enum ThreadType {
        /**
         * Default value used when the thread type is unrecognised by
         * the current client.
         */
        UNKNOWN(0),

        /**
         * A replies sub-thread grouping every reply directed at a
         * specific parent message.
         */
        VIEW_REPLIES(1),

        /**
         * A dedicated thread for a conversation with the WhatsApp AI
         * bot.
         */
        AI_THREAD(2);

        /**
         * Constructs a new enum constant with the given protobuf wire
         * index.
         *
         * @param index the protobuf wire index for this constant
         */
        ThreadType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index identifying this constant on the wire.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the non-negative wire index
         */
        public int index() {
            return this.index;
        }
    }
}
