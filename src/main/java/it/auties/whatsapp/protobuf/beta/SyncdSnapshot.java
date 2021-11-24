package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdSnapshot {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mac;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncdRecord")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdRecord> records;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("SyncdVersion")
  private SyncdVersion version;
}
