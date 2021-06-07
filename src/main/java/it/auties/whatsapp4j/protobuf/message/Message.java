package it.auties.whatsapp4j.protobuf.message;

/**
 * A model interface that represents a WhatsappMessage sent by a contact or by Whatsapp.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface Message permits CancelPaymentRequestMessage,
        ContextualMessage, DeclinePaymentRequestMessage, DeviceSentMessage,
        DeviceSyncMessage, HighlyStructuredMessage, ProductMessage,
        ProtocolMessage, RequestPaymentMessage, SendPaymentMessage,
        SenderKeyDistributionMessage, TemplateButtonReplyMessage, TemplateMessage {
}
