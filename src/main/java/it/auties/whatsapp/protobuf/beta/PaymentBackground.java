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
public class PaymentBackground {

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("fixed32")
  private int subtextArgb;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("fixed32")
  private int textArgb;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("fixed32")
  private int placeholderArgb;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String fileLength;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String id;
}
