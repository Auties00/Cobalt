package it.auties.whatsapp.protobuf.model.biz;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class VerifiedNameCertificate {
  @JsonProperty(value = "3")
  private byte[] serverSignature;

  @JsonProperty(value = "2")
  private byte[] signature;

  @JsonProperty(value = "1")
  private byte[] details;
}
