package it.auties.whatsapp4j.protobuf.message.model;

import it.auties.whatsapp4j.protobuf.message.security.SenderKeyDistributionMessage;

/**
 * A model interface that represents a WhatsappMessage sent by WhatsappWeb's Webserver for security reasons.
 * This messages follow ths Signal standard.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface SecurityMessage extends ServerMessage permits SenderKeyDistributionMessage {
}
