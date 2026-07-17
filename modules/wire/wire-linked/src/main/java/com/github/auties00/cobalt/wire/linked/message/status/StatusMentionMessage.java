package com.github.auties00.cobalt.wire.linked.message.status;

import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Envelope used by WhatsApp to deliver a status mention to the mentioned
 * user.
 *
 * <p>When someone mentions another user inside one of their status posts,
 * the platform sends a direct copy of the original status content to the
 * mentioned user's chat. This class carries that inlined copy as a
 * {@link LinkedMessageContainer}, allowing the recipient's client to render the
 * source status inline in their chat history.
 *
 * @see LinkedMessageContainer
 */
@ProtobufMessage(name = "StatusMentionMessage")
public final class StatusMentionMessage {
    /**
     * Inlined copy of the source status content that the user was mentioned
     * in.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    LinkedMessageContainer quotedStatus;

    /**
     * Constructs a new {@code StatusMentionMessage} wrapping the supplied
     * source status.
     *
     * @param quotedStatus the inlined copy of the source status, or
     *                     {@code null} if absent
     */
    StatusMentionMessage(LinkedMessageContainer quotedStatus) {
        this.quotedStatus = quotedStatus;
    }

    /**
     * Returns the inlined copy of the source status content.
     *
     * @return the source status container, or {@code Optional.empty()} if absent
     */
    public Optional<LinkedMessageContainer> quotedStatus() {
        return Optional.ofNullable(quotedStatus);
    }

    /**
     * Sets the inlined copy of the source status content.
     *
     * @param quotedStatus the source status container, or {@code null} to clear
     */
    public void setQuotedStatus(LinkedMessageContainer quotedStatus) {
        this.quotedStatus = quotedStatus;
    }
}
