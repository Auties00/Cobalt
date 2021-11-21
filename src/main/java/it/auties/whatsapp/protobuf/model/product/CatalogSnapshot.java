package it.auties.whatsapp.protobuf.model.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.message.standard.ImageMessage;
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
public class CatalogSnapshot {
  @JsonProperty(value = "3")
  private String description;

  @JsonProperty(value = "2")
  private String title;

  @JsonProperty(value = "1")
  private ImageMessage catalogImage;
}
