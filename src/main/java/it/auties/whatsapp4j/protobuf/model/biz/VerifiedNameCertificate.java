package it.auties.whatsapp4j.protobuf.model.biz;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
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
