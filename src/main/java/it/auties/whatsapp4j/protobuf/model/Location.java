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
public class Location {
  @JsonProperty(value = "3")
  private String name;

  @JsonProperty(value = "2")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  private double degreesLatitude;
}
