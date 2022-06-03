package it.auties.whatsapp.model.sync;


import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HistorySyncMessage implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageInfo.class)
    private MessageInfo message;

    @ProtobufProperty(index = 2, type = UINT64)
    private Long msgOrderId;
}
