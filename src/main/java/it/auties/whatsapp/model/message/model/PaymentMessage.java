package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.payment.CancelPaymentRequestMessage;
import it.auties.whatsapp.model.message.payment.DeclinePaymentRequestMessage;
import it.auties.whatsapp.model.message.payment.PaymentInviteMessage;
import it.auties.whatsapp.model.message.payment.PaymentInvoiceMessage;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.payment.RequestPaymentMessage;
import it.auties.whatsapp.model.message.payment.SendPaymentMessage;

/**
 * A model interface that represents a message regarding a payment
 */
public sealed interface PaymentMessage
    extends Message
    permits CancelPaymentRequestMessage, DeclinePaymentRequestMessage, PaymentInviteMessage,
    PaymentInvoiceMessage, PaymentOrderMessage, RequestPaymentMessage, SendPaymentMessage {

  @Override
  default MessageCategory category() {
    return MessageCategory.PAYMENT;
  }
}
