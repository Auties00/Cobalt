package it.auties.whatsapp.model.setting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class EphemeralSetting implements Setting {
  @ProtobufProperty(index = 1, type = SFIXED32)
  private float duration;

  @ProtobufProperty(index = 2, type = SFIXED64)
  private long timestamp;

  @Override
  public String indexName() {
    return "unknown";
  }
}
