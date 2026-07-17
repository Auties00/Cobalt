package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a message used to decline a payment request received from
 * a contact.
 *
 * <p>When a user receives a {@link RequestPaymentMessage} asking them
 * to pay a certain amount, they can respond with this message to
 * refuse the request. The message carries the {@link MessageKey} of
 * the original payment request so that the requester's client can
 * correlate the decline with the pending request and update the chat
 * UI to reflect that the request was refused.
 */
@ProtobufMessage(name = "Message.DeclinePaymentRequestMessage")
public final class DeclinePaymentRequestMessage implements Message {
    /**
     * The key of the original payment request message being declined.
     *
     * <p>Identifies the {@link RequestPaymentMessage} that this
     * decline refers to, allowing clients to locate and update the
     * pending request in the chat history.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;


    /**
     * Constructs a new decline message referencing the payment request
     * identified by the given key.
     *
     * @param key the key of the original payment request to decline,
     *            may be {@code null}
     */
    DeclinePaymentRequestMessage(MessageKey key) {
        this.key = key;
    }

    /**
     * Returns the key of the payment request being declined.
     *
     * @return an {@link Optional} containing the message key of the
     *         original payment request, or {@link Optional#empty()}
     *         if no key is present
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Sets the key of the payment request being declined.
     *
     * @param key the message key of the original payment request,
     *            may be {@code null}
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }
}
