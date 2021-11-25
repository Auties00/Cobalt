package it.auties.whatsapp.protobuf.authentication;

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
public class ADVSignedDeviceIdentity {
  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] deviceSignature;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] accountSignature;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] accountSignatureKey;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] details;
}
