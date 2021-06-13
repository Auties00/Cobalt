package it.auties.whatsapp4j.protobuf.message.model;

import it.auties.whatsapp4j.protobuf.message.business.CancelPaymentRequestMessage;
import it.auties.whatsapp4j.protobuf.message.business.DeclinePaymentRequestMessage;
import it.auties.whatsapp4j.protobuf.message.business.RequestPaymentMessage;
import it.auties.whatsapp4j.protobuf.message.business.SendPaymentMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a WhatsappBusiness account or by yourself regarding a payment.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface PaymentMessage extends BusinessMessage permits CancelPaymentRequestMessage, RequestPaymentMessage, SendPaymentMessage, DeclinePaymentRequestMessage {
}
