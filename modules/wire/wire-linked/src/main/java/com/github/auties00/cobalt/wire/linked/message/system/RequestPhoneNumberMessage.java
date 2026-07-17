package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message that asks the recipient to share their phone number inside
 * the current conversation.
 *
 * <p>WhatsApp uses this prompt in chats based on Linked Identifiers (LID)
 * where the phone number is hidden by default, such as community
 * announcements or certain privacy-preserving group chats, to let
 * participants explicitly request contact details from each other.
 *
 * <p>The message carries only the quoted-message context so that clients can
 * thread the request underneath the original interaction that triggered it.
 */
@ProtobufMessage(name = "Message.RequestPhoneNumberMessage")
public final class RequestPhoneNumberMessage implements ContextualMessage {
    /**
     * The conversational context attached to this request, including the
     * quoted message that prompted the user to ask for the recipient's phone
     * number, mention metadata, ephemeral-message hints and any other
     * contextual information that surrounds the request.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new request-phone-number message with the given context.
     *
     * @param contextInfo the conversational context, may be {@code null}
     */
    RequestPhoneNumberMessage(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the conversational context attached to this request.
     *
     * @return an {@link Optional} containing the {@link ContextInfo}, or
     *         {@link Optional#empty()} if no context is set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the conversational context attached to this request.
     *
     * @param contextInfo the new context, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
