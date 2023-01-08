package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
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
@ProtobufName("KeepInChat")
public class KeepInChat
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, name = "keepType", type = ProtobufType.MESSAGE)
  private KeepInChatType keepType;

  @ProtobufProperty(index = 2, name = "serverTimestamp", type = ProtobufType.INT64)
  private long serverTimestamp;

  @ProtobufProperty(index = 3, name = "key", type = ProtobufType.MESSAGE)
  private MessageKey key;

  @ProtobufProperty(index = 4, name = "deviceJid", type = ProtobufType.STRING)
  private ContactJid deviceJid;

  @ProtobufProperty(index = 5, name = "clientTimestampMs", type = ProtobufType.INT64)
  private long clientTimestampMilliseconds;

  @ProtobufProperty(index = 6, name = "serverTimestampMs", type = ProtobufType.INT64)
  private long serverTimestampMilliseconds;
}