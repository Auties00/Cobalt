package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
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
