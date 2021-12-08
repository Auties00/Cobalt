package it.auties.whatsapp.protobuf.group;

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
public class SenderSigningKey {
  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] publicKey;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] privateKey;
}
