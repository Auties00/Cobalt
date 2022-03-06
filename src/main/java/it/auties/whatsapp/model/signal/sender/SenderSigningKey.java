package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderSigningKey {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] publicKey;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] privateKey;
}
