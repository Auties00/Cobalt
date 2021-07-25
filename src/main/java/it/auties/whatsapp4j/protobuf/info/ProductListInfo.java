package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.product.ProductListHeaderImage;
import it.auties.whatsapp4j.protobuf.model.product.ProductSection;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ProductListInfo {
  @JsonProperty(value = "3")
  private String businessOwnerJid;

  @JsonProperty(value = "2")
  private ProductListHeaderImage headerImage;

  @JsonProperty(value = "1")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ProductSection> productSections;
}
