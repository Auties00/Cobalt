package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MsgRowOpaqueData {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("MsgOpaqueData")
  private MsgOpaqueData quotedMsg;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("MsgOpaqueData")
  private MsgOpaqueData currentMsg;
}
