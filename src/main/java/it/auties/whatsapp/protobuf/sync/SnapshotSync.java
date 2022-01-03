package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class SnapshotSync {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdVersion")
  private VersionSync version;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdRecord")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<RecordSync> records;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] mac;

  @JsonProperty("4")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;
}
