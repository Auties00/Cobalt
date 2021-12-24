package it.auties.whatsapp.protobuf.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class ProductCatalog {
  @JsonProperty("1")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage catalogImage;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String description;
}
