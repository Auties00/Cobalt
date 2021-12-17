package it.auties.whatsapp.protobuf.unknown;

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
public class ProductSnapshot {
  @JsonProperty(value = "12")
  @JsonPropertyDescription("int64")
  private long salePriceAmount1000;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("string")
  private String firstImageId;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("uint32")
  private int productImageCount;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String url;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String retailerId;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("int64")
  private long priceAmount1000;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String currencyCode;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String productId;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage productImage;
}
