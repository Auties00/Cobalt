package it.auties.whatsapp4j.protobuf.message.model;

import it.auties.whatsapp4j.protobuf.message.server.ProtocolMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a WhatsappWeb's server.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface ServerMessage extends Message permits SecurityMessage, ProtocolMessage {
}
