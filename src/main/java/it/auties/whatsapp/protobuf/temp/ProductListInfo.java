package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
  @JsonPropertyDescription("string")
  private String businessOwnerJid;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("ProductListHeaderImage")
  private ProductListHeaderImage headerImage;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("ProductSection")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ProductSection> productSections;
}
