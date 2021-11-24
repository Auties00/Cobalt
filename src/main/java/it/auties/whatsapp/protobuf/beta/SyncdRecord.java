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
public class SyncdRecord {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncdValue")
  private SyncdValue value;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("SyncdIndex")
  private SyncdIndex index;
}
