package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a message used to cancel a previously sent payment request.
 *
 * <p>When a user has asked a contact to pay a certain amount using
 * {@link RequestPaymentMessage}, this message can be sent afterwards to
 * retract that request before it is fulfilled. It carries the
 * {@link MessageKey} of the original payment request so that the
 * recipient's client can correlate the cancellation with the pending
 * request and update the chat UI accordingly.
 */
@ProtobufMessage(name = "Message.CancelPaymentRequestMessage")
public final class CancelPaymentRequestMessage implements Message {
    /**
     * The key of the original payment request message being cancelled.
     *
     * <p>Identifies the {@link RequestPaymentMessage} that this
     * cancellation refers to, allowing clients to locate and update
     * the pending request in the chat history.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;


    /**
     * Constructs a new cancellation message referencing the payment
     * request identified by the given key.
     *
     * @param key the key of the original payment request to cancel,
     *            may be {@code null}
     */
    CancelPaymentRequestMessage(MessageKey key) {
        this.key = key;
    }

    /**
     * Returns the key of the payment request being cancelled.
     *
     * @return an {@link Optional} containing the message key of the
     *         original payment request, or {@link Optional#empty()}
     *         if no key is present
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Sets the key of the payment request being cancelled.
     *
     * @param key the message key of the original payment request,
     *            may be {@code null}
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }
}
