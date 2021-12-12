package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PreKeyRecordStructure {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] privateKey;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] publicKey;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int id;
}
