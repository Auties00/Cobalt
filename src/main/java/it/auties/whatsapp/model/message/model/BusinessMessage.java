package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.business.*;

/**
 * A model interface that represents a message regarding whatsapp business
 */
public sealed interface BusinessMessage extends Message
        permits NativeFlowMessage, CollectionMessage, InteractiveMessage, ProductMessage, ShopMessage {
}
