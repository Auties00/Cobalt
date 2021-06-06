package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.message.ImageMessage;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class CatalogSnapshot {
  @JsonProperty(value = "3")
  private String description;

  @JsonProperty(value = "2")
  private String title;

  @JsonProperty(value = "1")
  private ImageMessage catalogImage;
}
