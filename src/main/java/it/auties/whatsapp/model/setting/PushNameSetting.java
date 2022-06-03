package it.auties.whatsapp.model.setting;

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
public final class PushNameSetting implements Setting {
  @ProtobufProperty(index = 1, type = STRING)
  private String name;

  @Override
  public String indexName() {
    return "setting_pushName";
  }
}
