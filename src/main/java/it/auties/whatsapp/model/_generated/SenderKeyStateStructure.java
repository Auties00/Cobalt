package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.signal.sender.SenderChainKey;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SenderKeyStateStructure")
public class SenderKeyStateStructure implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "senderKeyId", type = UINT32)
    private Integer senderKeyId;

    @ProtobufProperty(index = 2, name = "senderChainKey", type = MESSAGE)
    private SenderChainKey senderChainKey;

    @ProtobufProperty(index = 3, name = "senderSigningKey", type = MESSAGE)
    private SenderSigningKey senderSigningKey;

    @ProtobufProperty(implementation = SenderMessageKey.class, index = 4, name = "senderMessageKeys", repeated = true, type = MESSAGE)
    private List<SenderMessageKey> senderMessageKeys;
}