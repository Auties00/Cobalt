package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ContactAction implements Action {
  @ProtobufProperty(index = 1, type = STRING)
  private String fullName;

  @ProtobufProperty(index = 2, type = STRING)
  private String firstName;

  @Override
  public String indexName() {
    return "contact";
  }
}
