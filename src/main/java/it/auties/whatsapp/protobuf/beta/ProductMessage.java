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
public class ProductMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("CatalogSnapshot")
  private CatalogSnapshot catalog;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String businessOwnerJid;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("ProductSnapshot")
  private ProductSnapshot product;
}
