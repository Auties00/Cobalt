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
public class SyncdPatch {

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("uint32")
  private int deviceIndex;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("ExitCode")
  private ExitCode exitCode;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] patchMac;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] snapshotMac;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ExternalBlobReference")
  private ExternalBlobReference externalMutations;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SyncdMutation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdMutation> mutations;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("SyncdVersion")
  private SyncdVersion version;
}
