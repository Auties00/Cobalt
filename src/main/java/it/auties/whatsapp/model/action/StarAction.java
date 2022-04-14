package it.auties.whatsapp.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class StarAction implements Action {
  @ProtobufProperty(index = 1, type = BOOLEAN)
  private boolean starred;

  @Override
  public String indexName() {
    return "star";
  }
}
