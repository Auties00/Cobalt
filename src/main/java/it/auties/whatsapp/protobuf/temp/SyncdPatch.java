package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdPatch {
  @JsonProperty(value = "8")
  @JsonPropertyDescription("uint32")
  private int deviceIndex;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("ExitCode")
  private ExitCode exitCode;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("bytes")
  private byte[] patchMac;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] snapshotMac;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ExternalBlobReference")
  private ExternalBlobReference externalMutations;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SyncdMutation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdMutation> mutations;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("SyncdVersion")
  private SyncdVersion version;
}
