package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ButtonRow {
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String rowId;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String title;
}
