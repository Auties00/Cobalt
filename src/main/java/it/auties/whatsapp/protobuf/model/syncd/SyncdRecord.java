package it.auties.whatsapp.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.model.key.KeyId;
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
public class SyncdRecord {
  @JsonProperty(value = "3")
  private KeyId keyId;

  @JsonProperty(value = "2")
  private SyncdValue value;

  @JsonProperty(value = "1")
  private SyncdIndex index;
}
