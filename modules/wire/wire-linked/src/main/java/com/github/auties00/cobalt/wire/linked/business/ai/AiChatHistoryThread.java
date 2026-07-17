package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * One conversation of a chat-history backup uploaded to a WhatsApp Business AI
 * agent.
 *
 * <p>A chat-history backup groups past messages by the customer they were
 * exchanged with. This model is one such conversation: it carries the
 * {@link #consumerUid() consumer identifier} naming the other party and the
 * ordered list of {@link #messages() messages} exchanged with them.
 */
@ProtobufMessage(name = "AiChatHistoryThread")
public final class AiChatHistoryThread {
    /**
     * Identifier of the consumer this conversation was held with. Empty when
     * unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String consumerUid;

    /**
     * Messages exchanged in this conversation, in the order they were sent.
     * Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<AiChatHistoryMessage> messages;

    /**
     * Constructs a new {@code AiChatHistoryThread}. A {@code null}
     * {@code messages} is coerced to an empty list; {@code consumerUid} may be
     * {@code null} to leave it unset.
     *
     * @param consumerUid the consumer identifier, or {@code null}
     * @param messages    the conversation messages; {@code null} treated as empty
     */
    AiChatHistoryThread(String consumerUid, List<AiChatHistoryMessage> messages) {
        this.consumerUid = consumerUid;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
    }

    /**
     * Returns the identifier of the consumer this conversation was held with.
     *
     * @return an {@link Optional} carrying the consumer identifier, or empty
     *         when unset
     */
    public Optional<String> consumerUid() {
        return Optional.ofNullable(consumerUid);
    }

    /**
     * Returns the messages exchanged in this conversation.
     *
     * @return an unmodifiable view of the messages; never {@code null}, possibly
     *         empty
     */
    public List<AiChatHistoryMessage> messages() {
        return messages;
    }
}
