package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
  @Deprecated
  private int yDeprecated;

  @JsonProperty(value = "1")
  @Deprecated
  private int xDeprecated;
}
