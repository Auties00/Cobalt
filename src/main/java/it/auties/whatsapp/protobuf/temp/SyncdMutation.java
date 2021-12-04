package it.auties.whatsapp.protobuf.temp;

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
public class SyncdMutation {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("SyncdRecord")
  private SyncdRecord record;

  @JsonProperty(value = "1")
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
