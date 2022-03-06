package it.auties.whatsapp.model.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
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
