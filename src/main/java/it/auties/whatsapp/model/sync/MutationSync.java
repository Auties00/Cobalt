package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class MutationSync {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdMutationSyncdOperation")
  private RecordSync.Operation operation;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdRecord")
  private RecordSync record;
}
