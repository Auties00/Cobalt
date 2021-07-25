package it.auties.whatsapp4j.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.model.key.KeyId;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdRecord {
  @JsonProperty(value = "3")
  private KeyId keyId;

  @JsonProperty(value = "2")
  private SyncdValue value;

  @JsonProperty(value = "1")
  private SyncdIndex index;
}
