package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Point {
  @JsonProperty(value = "4")
  private double y;

  @JsonProperty(value = "3")
  private double x;

  @JsonProperty(value = "2")
  private int yDeprecated;

  @JsonProperty(value = "1")
  private int xDeprecated;
}
