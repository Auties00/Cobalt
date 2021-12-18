package it.auties.whatsapp.protobuf.message.model;

import it.auties.whatsapp.protobuf.message.payment.*;

/**
 * A model interface that represents a WhatsappMessage regarding a payment
 */
public sealed interface PaymentMessage extends Message permits CancelPaymentRequestMessage, DeclinePaymentRequestMessage, PaymentInviteMessage, PaymentInvoiceMessage, PaymentOrderMessage, RequestPaymentMessage, SendPaymentMessage {
}
