package it.auties.whatsapp.protobuf.model.action;

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
public class MuteAction {
  @JsonProperty(value = "2")
  private long muteEndTimestamp;

  @JsonProperty(value = "1")
  private boolean muted;
}
