package it.auties.whatsapp4j.protobuf.message.model;

import it.auties.whatsapp4j.protobuf.message.business.HighlyStructuredMessage;
import it.auties.whatsapp4j.protobuf.message.business.ProductMessage;
import it.auties.whatsapp4j.protobuf.message.business.TemplateButtonReplyMessage;
import it.auties.whatsapp4j.protobuf.message.business.TemplateMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a WhatsappBusiness account.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface BusinessMessage extends Message permits HighlyStructuredMessage, ProductMessage,
        TemplateButtonReplyMessage, TemplateMessage, PaymentMessage {
}
