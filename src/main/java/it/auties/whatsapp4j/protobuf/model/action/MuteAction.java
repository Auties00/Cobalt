package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MuteAction {
  @JsonProperty(value = "2")
  private long muteEndTimestamp;

  @JsonProperty(value = "1")
  private boolean muted;
}
