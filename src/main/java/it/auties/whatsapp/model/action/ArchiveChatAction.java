package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ArchiveChatAction implements Action {
  @ProtobufProperty(index = 1, type = BOOLEAN)
  private boolean archived;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ActionMessageRangeSync.class)
  private ActionMessageRangeSync messageRange;

  @Override
  public String indexName() {
    return "archive";
  }
}
