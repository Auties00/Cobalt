package it.auties.whatsapp.protobuf.sync;

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
public class SyncdSnapshot {
  @JsonProperty("4")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] mac;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdRecord")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncdRecord> records;

  @JsonProperty("1")
  @JsonPropertyDescription("SyncdVersion")
  private SyncdVersion version;
}
