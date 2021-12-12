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
public class SignedPreKeyRecordStructure {
  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] publicKey;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] privateKey;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("fixed64")
  private long timestamp;
}
