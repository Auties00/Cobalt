package it.auties.whatsapp4j.protobuf.model.biz;

import com.fasterxml.jackson.annotation.*;

import lombok.*;
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
