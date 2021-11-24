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
public class Row {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String rowId;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String title;
}
