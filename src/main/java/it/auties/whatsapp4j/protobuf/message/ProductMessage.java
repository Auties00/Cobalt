package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.model.CatalogSnapshot;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.miscellanous.ProductSnapshot;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class ProductMessage implements Message {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "4")
  private CatalogSnapshot catalog;

  @JsonProperty(value = "2")
  private String businessOwnerJid;

  @JsonProperty(value = "1")
  private ProductSnapshot product;
}
