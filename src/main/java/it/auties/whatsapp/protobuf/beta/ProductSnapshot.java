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
public class ProductSnapshot {

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("int64")
  private long salePriceAmount1000;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("string")
  private String firstImageId;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("uint32")
  private int productImageCount;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String url;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String retailerId;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("int64")
  private long priceAmount1000;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String currencyCode;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String productId;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage productImage;
}
