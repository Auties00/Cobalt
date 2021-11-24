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
public class Location {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String name;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("double")
  private double degreesLongitude;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("double")
  private double degreesLatitude;
}
