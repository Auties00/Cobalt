package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.message.server.*;
import com.github.auties00.cobalt.model.message.standard.EncryptedReactionMessage;

/**
 * A model interface that represents a message sent by a WhatsappWeb's server
 */
public sealed interface ServerMessage extends Message permits DeviceSentMessage, DeviceSyncMessage, EncryptedReactionMessage, ProtocolMessage, SenderKeyDistributionMessage, StickerSyncRMRMessage {
    @Override
    default Category category() {
        return Category.SERVER;
    }
}
