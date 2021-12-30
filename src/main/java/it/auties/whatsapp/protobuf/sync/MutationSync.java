package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MutationSync {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdMutationSyncdOperation")
  private SyncdMutationSyncdOperation operation;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdRecord")
  private RecordSync record;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum SyncdMutationSyncdOperation {
    SET(0),
    REMOVE(1);

    @Getter
    private final int index;

    @JsonCreator
    public static SyncdMutationSyncdOperation forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
