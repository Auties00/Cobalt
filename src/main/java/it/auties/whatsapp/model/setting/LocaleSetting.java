package it.auties.whatsapp.model.setting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class LocaleSetting implements Setting {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String locale;

  @Override
  public String indexName() {
    return "unknown";
  }
}
