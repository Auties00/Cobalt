package it.auties.whatsapp.protobuf.business;

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
public class BusinessCertificate {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] serverSignature;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] details;
}
