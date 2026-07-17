package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Input model for uploading a chat-history backup to a WhatsApp Business AI
 * agent.
 *
 * <p>A merchant can seed their auto-reply assistant from past conversations by
 * uploading them in bulk. This model is the whole upload: an ordered list of
 * {@link #threads() conversations}, each grouping the messages exchanged with
 * one customer.
 */
@ProtobufMessage(name = "AiChatHistoryUploadRequest")
public final class AiChatHistoryUploadRequest {
    /**
     * Conversations included in this upload, in the order they are sent. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<AiChatHistoryThread> threads;

    /**
     * Constructs a new {@code AiChatHistoryUploadRequest}. A {@code null}
     * {@code threads} is coerced to an empty list.
     *
     * @param threads the conversations to upload; {@code null} treated as empty
     */
    AiChatHistoryUploadRequest(List<AiChatHistoryThread> threads) {
        this.threads = threads == null ? List.of() : List.copyOf(threads);
    }

    /**
     * Returns the conversations included in this upload.
     *
     * @return an unmodifiable view of the conversations; never {@code null},
     *         possibly empty
     */
    public List<AiChatHistoryThread> threads() {
        return threads;
    }
}
