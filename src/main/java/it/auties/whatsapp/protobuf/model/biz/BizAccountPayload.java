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
public class BizAccountPayload {
  @JsonProperty(value = "2")
  private byte[] bizAcctLinkInfo;

  @JsonProperty(value = "1")
  private VerifiedNameCertificate vnameCert;
}
