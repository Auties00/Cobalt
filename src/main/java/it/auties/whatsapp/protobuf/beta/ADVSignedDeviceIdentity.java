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
public class ADVSignedDeviceIdentity {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] deviceSignature;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] accountSignature;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] accountSignatureKey;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] details;
}
