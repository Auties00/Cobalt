package it.auties.whatsapp.model.message.model;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
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

  @ProtobufProperty(index = 1, name = "keepType", type = MESSAGE)
  private KeepInChatType keepType;

  @ProtobufProperty(index = 2, name = "serverTimestamp", type = INT64)
  private long serverTimestamp;

  @ProtobufProperty(index = 3, name = "key", type = MESSAGE)
  private MessageKey key;

  @ProtobufProperty(index = 4, name = "deviceJid", type = STRING)
  private ContactJid deviceJid;

  @ProtobufProperty(index = 5, name = "clientTimestampMs", type = INT64)
  private long clientTimestampMilliseconds;

  @ProtobufProperty(index = 6, name = "serverTimestampMs", type = INT64)
  private long serverTimestampMilliseconds;
}