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
public class SyncdMutation {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncdRecord")
  private SyncdRecord record;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("SyncdMutationSyncdOperation")
  private SyncdMutationSyncdOperation operation;

  @Accessors(fluent = true)
  public enum SyncdMutationSyncdOperation {
    SET(0),
    REMOVE(1);

    private final @Getter int index;

    SyncdMutationSyncdOperation(int index) {
      this.index = index;
    }

    @JsonCreator
    public static SyncdMutationSyncdOperation forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
