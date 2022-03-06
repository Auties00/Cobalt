package it.auties.whatsapp.model.button;

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
public class RowOpaqueData {
  @JsonProperty("1")
  @JsonPropertyDescription("MsgOpaqueData")
  private ButtonOpaqueData currentMsg;

  @JsonProperty("2")
  @JsonPropertyDescription("MsgOpaqueData")
  private ButtonOpaqueData quotedMsg;
}
