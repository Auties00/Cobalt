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
public class MsgOpaqueData {

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("string")
  private String paymentNoteMsgBody;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("int32")
  private int paymentAmount1000;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("double")
  private double lat;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("double")
  private double lng;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String clientUrl;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String body;
}
