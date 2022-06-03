package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class DeleteMessageForMeAction implements Action {
  @ProtobufProperty(index = 1, type = BOOLEAN)
  private boolean deleteMedia;

  @ProtobufProperty(index = 2, type = INT64)
  private Long messageTimestamp;

  @Override
  public String indexName() {
    return "deleteMessageForMe";
  }
}
