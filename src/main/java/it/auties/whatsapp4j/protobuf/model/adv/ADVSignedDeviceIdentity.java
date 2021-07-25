package it.auties.whatsapp4j.protobuf.model.adv;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ADVSignedDeviceIdentity {
  @JsonProperty(value = "4")
  private byte[] deviceSignature;

  @JsonProperty(value = "3")
  private byte[] accountSignature;

  @JsonProperty(value = "2")
  private byte[] accountSignatureKey;

  @JsonProperty(value = "1")
  private byte[] details;
}
