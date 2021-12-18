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
public class RowOpaqueData {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("MsgOpaqueData")
  private ButtonOpaqueData quotedMsg;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("MsgOpaqueData")
  private ButtonOpaqueData currentMsg;
}
