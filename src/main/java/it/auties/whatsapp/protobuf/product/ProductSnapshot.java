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
public class ProductSnapshot {
  @JsonProperty("1")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage productImage;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String productId;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String currencyCode;

  @JsonProperty("6")
  @JsonPropertyDescription("int64")
  private long priceAmount1000;

  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String retailerId;

  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String url;

  @JsonProperty("9")
  @JsonPropertyDescription("uint32")
  private int productImageCount;

  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String firstImageId;

  @JsonProperty("12")
  @JsonPropertyDescription("int64")
  private long salePriceAmount1000;
}
