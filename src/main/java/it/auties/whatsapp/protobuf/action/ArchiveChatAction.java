package it.auties.whatsapp.protobuf.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.sync.ActionMessageRangeSync;
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
public final class ArchiveChatAction implements Action {
  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean archived;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncActionMessageRange")
  private ActionMessageRangeSync messageRange;
}
