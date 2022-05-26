package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ActionDataSync implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] index;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ActionValueSync.class)
  private ActionValueSync value;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] padding;

  @ProtobufProperty(index = 4, type = INT32)
  private Integer version;

  public MessageSync messageIndex(){
    var jsonIndex = new String(index, StandardCharsets.UTF_8);
    return MessageSync.ofJson(jsonIndex);
  }
}
