package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.server.DeviceSentMessage;
import it.auties.whatsapp.model.message.server.DeviceSyncMessage;
import it.auties.whatsapp.model.message.server.EncryptedReactionMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.server.StickerSyncRMRMessage;

/**
 * A model interface that represents a message sent by a WhatsappWeb's server
 */
public sealed interface ServerMessage
    extends Message
    permits DeviceSentMessage, DeviceSyncMessage, EncryptedReactionMessage, ProtocolMessage,
    SenderKeyDistributionMessage, StickerSyncRMRMessage {

  @Override
  default MessageCategory category() {
    return MessageCategory.SERVER;
  }
}
