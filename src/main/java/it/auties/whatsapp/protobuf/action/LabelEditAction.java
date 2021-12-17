package it.auties.whatsapp.protobuf.action;

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
public class LabelEditAction {
  @JsonProperty(value = "4")
  @JsonPropertyDescription("bool")
  private boolean deleted;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("int32")
  private int predefinedId;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("int32")
  private int color;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String name;
}
