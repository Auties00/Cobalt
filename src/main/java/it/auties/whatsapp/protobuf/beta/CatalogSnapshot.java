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
public class CatalogSnapshot {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage catalogImage;
}
