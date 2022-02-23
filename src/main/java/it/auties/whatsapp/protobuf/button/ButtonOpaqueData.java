package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class ButtonOpaqueData {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String body;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String clientUrl;

  @JsonProperty("5")
  @JsonPropertyDescription("double")
  private double lng;

  @JsonProperty("7")
  @JsonPropertyDescription("double")
  private double lat;

  @JsonProperty("8")
  @JsonPropertyDescription("int32")
  private int paymentAmount1000;

  @JsonProperty("9")
  @JsonPropertyDescription("string")
  private String paymentNoteMsgBody;

  @JsonProperty("10")
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty("12")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty("13")
  @JsonPropertyDescription("string")
  private String description;
}
