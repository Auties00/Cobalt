package it.auties.whatsapp.protobuf.unknown;

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
public class BizAccountPayload {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] bizAcctLinkInfo;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("VerifiedNameCertificate")
  private VerifiedNameCertificate vnameCert;
}
