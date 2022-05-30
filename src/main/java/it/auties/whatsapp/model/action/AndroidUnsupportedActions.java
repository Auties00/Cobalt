package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class AndroidUnsupportedActions implements Action {
  @ProtobufProperty(index = 1, type = BOOLEAN)
  private boolean allowed;

  @Override
  public String indexName() {
    return "android_unsupported_actions";
  }
}
