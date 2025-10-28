package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.message.payment.*;
import com.github.auties00.cobalt.model.message.payment.*;
import com.github.auties00.cobalt.model.message.payment.*;

/**
 * A model interface that represents a message regarding a payment
 */
public sealed interface PaymentMessage extends Message permits CancelPaymentRequestMessage, DeclinePaymentRequestMessage, PaymentInviteMessage, PaymentInvoiceMessage, PaymentOrderMessage, RequestPaymentMessage, SendPaymentMessage {
    @Override
    default Category category() {
        return Category.PAYMENT;
    }
}
