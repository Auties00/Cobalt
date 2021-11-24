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
public class VerifiedNameCertificate {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] serverSignature;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] details;
}
