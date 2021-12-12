package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class IdentityKeyPairStructure {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] privateKey;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] publicKey;
}
