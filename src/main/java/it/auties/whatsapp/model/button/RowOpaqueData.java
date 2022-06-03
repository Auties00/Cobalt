package it.auties.whatsapp.model.button;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class RowOpaqueData implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ButtonOpaqueData.class)
  private ButtonOpaqueData currentMsg;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ButtonOpaqueData.class)
  private ButtonOpaqueData quotedMsg;
}
