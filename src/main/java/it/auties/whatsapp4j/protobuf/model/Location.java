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
public class Location {
  @JsonProperty(value = "3")
  private String name;

  @JsonProperty(value = "2")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  private double degreesLatitude;
}
