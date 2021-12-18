package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.message.button.StructuredButtonMessage;
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
public class QuickReplyButton {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String id;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("HighlyStructuredMessage")
  private StructuredButtonMessage displayText;
}
