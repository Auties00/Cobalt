package com.github.auties00.cobalt.model.proto.message.model;

import com.github.auties00.cobalt.model.core.proto.message.payment.*;
import com.github.auties00.cobalt.model.proto.message.payment.*;
import com.github.auties00.cobalt.model.support.proto.message.payment.*;

/**
 * A model interface that represents a message regarding a payment
 */
public sealed interface PaymentMessage extends Message permits CancelPaymentRequestMessage, DeclinePaymentRequestMessage, PaymentInviteMessage, PaymentInvoiceMessage, PaymentOrderMessage, RequestPaymentMessage, SendPaymentMessage {
    @Override
    default Category category() {
        return Category.PAYMENT;
    }
}
