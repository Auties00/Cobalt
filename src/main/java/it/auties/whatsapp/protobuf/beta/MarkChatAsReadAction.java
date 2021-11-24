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
public class MarkChatAsReadAction {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncActionMessageRange")
  private SyncActionMessageRange messageRange;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bool")
  private boolean read;
}
