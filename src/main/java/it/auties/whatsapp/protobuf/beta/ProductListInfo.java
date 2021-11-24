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
public class ProductListInfo {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String businessOwnerJid;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("ProductListHeaderImage")
  private ProductListHeaderImage headerImage;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("ProductSection")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ProductSection> productSections;
}
