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
public class InteractiveAnnotation {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Location")
  private Location location;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("Point")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Point> polygonVertices;

  public Action actionCase() {
    if (location != null) return Action.LOCATION;
    return Action.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Action {
    UNKNOWN(0),
    LOCATION(2);

    private final @Getter int index;

    Action(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Action forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Action.UNKNOWN);
    }
  }
}
