package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.button.StructuredButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class CallButton implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = StructuredButtonMessage.class)
  private StructuredButtonMessage displayText;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = StructuredButtonMessage.class)
  private StructuredButtonMessage phoneNumber;
}
