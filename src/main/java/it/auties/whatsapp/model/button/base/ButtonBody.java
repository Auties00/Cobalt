package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.info.NativeFlowInfo;

/**
 * A model that represents the body of a button
 */
public sealed interface ButtonBody extends ProtobufMessage permits ButtonText, NativeFlowInfo {
    /**
     * Returns the type of this body
     *
     * @return a non-null type
     */
    ButtonBodyType bodyType();
}
