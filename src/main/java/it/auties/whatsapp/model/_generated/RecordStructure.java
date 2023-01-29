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
@ProtobufName("RecordStructure")
public class RecordStructure implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "currentSession", type = ProtobufType.MESSAGE)
  private SessionStructure currentSession;

  @ProtobufProperty(implementation = SessionStructure.class, index = 2, name = "previousSessions", repeated = true, type = ProtobufType.MESSAGE)
  private List<SessionStructure> previousSessions;
}