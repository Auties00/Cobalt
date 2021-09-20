package it.auties.whatsapp4j.common.protobuf.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.model.product.ProductListHeaderImage;
import it.auties.whatsapp4j.common.protobuf.model.product.ProductSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

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
