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
public class SyncActionData {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("int32")
  private int version;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] padding;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncActionValue")
  private SyncActionValue value;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] index;
}
