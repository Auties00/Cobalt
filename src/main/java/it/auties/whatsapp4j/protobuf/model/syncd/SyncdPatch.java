package it.auties.whatsapp4j.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.client.ExitCode;
import it.auties.whatsapp4j.protobuf.model.key.KeyId;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdPatch {
  @JsonProperty(value = "8")
  private int deviceIndex;

  @JsonProperty(value = "7")
  private ExitCode exitCode;

  @JsonProperty(value = "6")
  private KeyId keyId;

  @JsonProperty(value = "5")
  private byte[] patchMac;

  @JsonProperty(value = "4")
  private byte[] snapshotMac;

  @JsonProperty(value = "3")
  private ExternalBlobReference externalMutations;

  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdMutation> mutations;

  @JsonProperty(value = "1")
  private SyncdVersion version;
}
