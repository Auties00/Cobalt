package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.signal.sender.SenderChainKey;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SenderKeyStateStructure")
public class SenderKeyStateStructure implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "senderKeyId", type = ProtobufType.UINT32)
  private Integer senderKeyId;

  @ProtobufProperty(index = 2, name = "senderChainKey", type = ProtobufType.MESSAGE)
  private SenderChainKey senderChainKey;

  @ProtobufProperty(index = 3, name = "senderSigningKey", type = ProtobufType.MESSAGE)
  private SenderSigningKey senderSigningKey;

  @ProtobufProperty(implementation = SenderMessageKey.class, index = 4, name = "senderMessageKeys", repeated = true, type = ProtobufType.MESSAGE)
  private List<SenderMessageKey> senderMessageKeys;
}