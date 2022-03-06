package it.auties.whatsapp.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
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
public final class ArchiveChatAction implements Action {
  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean archived;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncActionMessageRange")
  private ActionMessageRangeSync messageRange;

  @Override
  public String indexName() {
    return "archive";
  }
}
