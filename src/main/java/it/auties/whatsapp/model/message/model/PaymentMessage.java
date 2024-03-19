package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.payment.*;

/**
 * A model interface that represents a message regarding a payment
 */
public sealed interface PaymentMessage extends Message permits CancelPaymentRequestMessage, DeclinePaymentRequestMessage, PaymentInviteMessage, PaymentInvoiceMessage, PaymentOrderMessage, RequestPaymentMessage, SendPaymentMessage {
    @Override
    default MessageCategory category() {
        return MessageCategory.PAYMENT;
    }
}
