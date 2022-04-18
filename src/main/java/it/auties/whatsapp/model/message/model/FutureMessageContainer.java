package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
class FutureMessageContainer implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageContainer.class)
    protected MessageContainer content;

    protected static FutureMessageContainer of(MessageContainer container){
        return new FutureMessageContainer(container);
    }
}
