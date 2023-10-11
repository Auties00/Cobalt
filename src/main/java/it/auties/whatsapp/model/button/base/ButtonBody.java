package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
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
    Type bodyType();

    enum Type implements ProtobufEnum {
        UNKNOWN(0),
        TEXT(1),
        NATIVE_FLOW(2);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
