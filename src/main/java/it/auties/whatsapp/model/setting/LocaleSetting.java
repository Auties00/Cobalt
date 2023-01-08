package it.auties.whatsapp.model.setting;

import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class LocaleSetting
    implements Setting {

  @ProtobufProperty(index = 1, type = STRING)
  private String locale;

  @Override
  public String indexName() {
    return "setting_locale";
  }
}
