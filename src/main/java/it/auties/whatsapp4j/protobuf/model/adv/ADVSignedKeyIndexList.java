package it.auties.whatsapp4j.protobuf.model.adv;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ADVSignedKeyIndexList {
  @JsonProperty(value = "2")
  private byte[] accountSignature;

  @JsonProperty(value = "1")
  private byte[] details;
}
