package it.auties.whatsapp.protobuf.unknown;

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
public class MsgOpaqueData {
  @JsonProperty(value = "13")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String paymentNoteMsgBody;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("int32")
  private int paymentAmount1000;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("double")
  private double lat;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("double")
  private double lng;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String clientUrl;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String body;
}
