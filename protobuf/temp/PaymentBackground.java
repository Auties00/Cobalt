package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class PaymentBackground {
  @JsonProperty(value = "8")
  @JsonPropertyDescription("fixed32")
  private int subtextArgb;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("fixed32")
  private int textArgb;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("fixed32")
  private int placeholderArgb;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String fileLength;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String id;
}
