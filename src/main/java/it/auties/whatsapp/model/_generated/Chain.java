package it.auties.whatsapp.model._generated;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("Chain")
public class Chain implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "senderRatchetKey", type = BYTES)
  private byte[] senderRatchetKey;

  @ProtobufProperty(index = 2, name = "senderRatchetKeyPrivate", type = BYTES)
  private byte[] senderRatchetKeyPrivate;

  @ProtobufProperty(index = 3, name = "chainKey", type = MESSAGE)
  private ChainKey chainKey;

  @ProtobufProperty(implementation = MessageKey.class, index = 4, name = "messageKeys", repeated = true, type = MESSAGE)
  private List<MessageKey> messageKeys;
}