package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.server.*;
import it.auties.whatsapp.model.message.standard.EncryptedReactionMessage;

/**
 * A model interface that represents a message sent by a WhatsappWeb's server
 */
public sealed interface ServerMessage extends Message permits DeviceSentMessage, DeviceSyncMessage, EncryptedReactionMessage, ProtocolMessage, SenderKeyDistributionMessage, StickerSyncRMRMessage {
    @Override
    default MessageCategory category() {
        return MessageCategory.SERVER;
    }
}
