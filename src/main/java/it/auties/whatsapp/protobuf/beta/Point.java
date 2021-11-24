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
public class Point {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("double")
  private double y;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("double")
  private double x;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int32")
  private int yDeprecated;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("int32")
  private int xDeprecated;
}