package it.auties.whatsapp.protobuf.temp;

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
public class TemplateButtonReplyMessage {
  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint32")
  private int selectedIndex;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String selectedDisplayText;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String selectedId;
}
