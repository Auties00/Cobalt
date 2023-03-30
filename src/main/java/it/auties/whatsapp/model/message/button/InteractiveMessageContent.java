package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.interactive.InteractiveCollection;
import it.auties.whatsapp.model.interactive.InteractiveNativeFlow;
import it.auties.whatsapp.model.interactive.InteractiveShop;

/**
 * A model class that represents a message that can be used as the content of a {@link it.auties.whatsapp.model.message.button.InteractiveMessage}
 */
public sealed interface InteractiveMessageContent extends ProtobufMessage permits InteractiveShop, InteractiveCollection, InteractiveNativeFlow {
    /**
     * Returns the type of this content
     *
     * @return a non-null type
     */
    InteractiveMessageContentType contentType();
}
