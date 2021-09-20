package it.auties.whatsapp4j.common.protobuf.model.adv;

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
public class ADVSignedKeyIndexList {
  @JsonProperty(value = "2")
  private byte[] accountSignature;

  @JsonProperty(value = "1")
  private byte[] details;
}
