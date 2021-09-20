package it.auties.whatsapp4j.common.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdMutation {
  @JsonProperty(value = "2")
  private SyncdRecord record;

  @JsonProperty(value = "1")
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
