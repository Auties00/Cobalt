package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * The set of knowledge items a WhatsApp Business AI agent has inferred and
 * is awaiting its operator's approval on.
 *
 * <p>The WhatsApp Business AI agent (the merchant's auto-reply assistant)
 * learns facts about the business as it answers chats. Before adopting an
 * inferred fact it queues it for review, so the operator can keep accurate
 * items and discard incorrect ones. This model is the review queue: the
 * collection of {@link BusinessAiPendingKnowledge} items currently waiting
 * for a decision.
 */
@ProtobufMessage(name = "BusinessAiKnowledgeReview")
public final class BusinessAiKnowledgeReview {
    /**
     * Knowledge items the assistant inferred and queued for the operator to
     * keep or discard, in the order the server returned them. Never
     * {@code null}, possibly empty when nothing is awaiting review.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<BusinessAiPendingKnowledge> pendingItems;

    /**
     * Constructs a new {@code BusinessAiKnowledgeReview}. A {@code null}
     * {@code pendingItems} is coerced to an empty list.
     *
     * @param pendingItems the queued knowledge items; {@code null} treated
     *                     as empty
     */
    BusinessAiKnowledgeReview(List<BusinessAiPendingKnowledge> pendingItems) {
        this.pendingItems = pendingItems == null ? List.of() : pendingItems;
    }

    /**
     * Returns the knowledge items awaiting the operator's approval.
     *
     * @return an unmodifiable view of the queued items; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiPendingKnowledge> pendingItems() {
        return Collections.unmodifiableList(pendingItems);
    }
}
