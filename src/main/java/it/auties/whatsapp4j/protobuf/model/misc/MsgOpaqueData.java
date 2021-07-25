package it.auties.whatsapp4j.protobuf.model.misc;

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
  @JsonProperty(value = "13")
  private String description;

  @JsonProperty(value = "12")
  private String title;

  @JsonProperty(value = "11")
  private String matchedText;

  @JsonProperty(value = "10")
  private String canonicalUrl;

  @JsonProperty(value = "9")
  private String paymentNoteMsgBody;

  @JsonProperty(value = "8")
  private int paymentAmount1000;

  @JsonProperty(value = "7")
  private double lat;

  @JsonProperty(value = "5")
  private double lng;

  @JsonProperty(value = "4")
  private String clientUrl;

  @JsonProperty(value = "3")
  private String caption;

  @JsonProperty(value = "1")
  private String body;
}
