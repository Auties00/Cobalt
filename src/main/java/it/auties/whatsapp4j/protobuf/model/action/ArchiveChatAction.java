package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;

import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ArchiveChatAction {
  @JsonProperty(value = "2")
  private SyncActionMessageRange messageRange;

  @JsonProperty(value = "1")
  private boolean archived;
}
