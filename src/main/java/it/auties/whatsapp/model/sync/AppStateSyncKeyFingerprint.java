package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.annotation.ProtobufPacked;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class AppStateSyncKeyFingerprint {
  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int rawId;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int currentIndex;

  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @ProtobufPacked
  private List<Integer> deviceIndexes;
}
