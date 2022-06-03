package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a WhatsappWeb's server
 */
public sealed interface ServerMessage extends Message permits ProtocolMessage, SenderKeyDistributionMessage {
}
