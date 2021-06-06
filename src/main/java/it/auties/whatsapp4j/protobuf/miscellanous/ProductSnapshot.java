package it.auties.whatsapp4j.protobuf.miscellanous;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.message.ImageMessage;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ProductSnapshot {
  @JsonProperty(value = "11")
  private String firstImageId;

  @JsonProperty(value = "9")
  private int productImageCount;

  @JsonProperty(value = "8")
  private String url;

  @JsonProperty(value = "7")
  private String retailerId;

  @JsonProperty(value = "6")
  private long priceAmount1000;

  @JsonProperty(value = "5")
  private String currencyCode;

  @JsonProperty(value = "4")
  private String description;

  @JsonProperty(value = "3")
  private String title;

  @JsonProperty(value = "2")
  private String productId;

  @JsonProperty(value = "1")
  private ImageMessage productImage;
}
