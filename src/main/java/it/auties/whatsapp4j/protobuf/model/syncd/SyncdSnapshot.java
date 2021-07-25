package it.auties.whatsapp4j.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.key.KeyId;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdSnapshot {
  @JsonProperty(value = "4")
  private KeyId keyId;

  @JsonProperty(value = "3")
  private byte[] mac;

  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdRecord> records;

  @JsonProperty(value = "1")
  private SyncdVersion version;
}
