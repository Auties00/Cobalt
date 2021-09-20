package it.auties.whatsapp4j.common.protobuf.model.misc;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MsgRowOpaqueData {
  @JsonProperty(value = "2")
  private MsgOpaqueData quotedMsg;

  @JsonProperty(value = "1")
  private MsgOpaqueData currentMsg;
}
