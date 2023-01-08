package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.message.model.KeepInChatType;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("KeepInChatMessage")
public final class KeepInChatMessage
    implements Message {

  @ProtobufProperty(index = 1, name = "key", type = ProtobufType.MESSAGE)
  private MessageKey key;

  @ProtobufProperty(index = 2, name = "keepType", type = ProtobufType.MESSAGE)
  private KeepInChatType keepType;

  @ProtobufProperty(index = 3, name = "timestampMs", type = ProtobufType.INT64)
  private long timestampMilliseconds;

  @Override
  public MessageType type() {
    return MessageType.KEEP_IN_CHAT;
  }

  @Override
  public MessageCategory category() {
    return MessageCategory.STANDARD;
  }
}
