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
public class Call {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int conversionDelaySeconds;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] conversionData;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String conversionSource;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] callKey;
}
