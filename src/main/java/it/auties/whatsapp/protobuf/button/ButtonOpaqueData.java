package it.auties.whatsapp.protobuf.button;

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
public class ButtonOpaqueData {
  @JsonProperty("13")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty("12")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty("10")
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty("9")
  @JsonPropertyDescription("string")
  private String paymentNoteMsgBody;

  @JsonProperty("8")
  @JsonPropertyDescription("int32")
  private int paymentAmount1000;

  @JsonProperty("7")
  @JsonPropertyDescription("double")
  private double lat;

  @JsonProperty("5")
  @JsonPropertyDescription("double")
  private double lng;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String clientUrl;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String body;
}
