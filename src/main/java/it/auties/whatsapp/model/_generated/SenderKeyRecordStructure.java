package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SenderKeyRecordStructure")
public class SenderKeyRecordStructure implements ProtobufMessage {
  @ProtobufProperty(implementation = SenderKeyStateStructure.class, index = 1, name = "senderKeyStates", repeated = true, type = ProtobufType.MESSAGE)
  private List<SenderKeyStateStructure> senderKeyStates;
}