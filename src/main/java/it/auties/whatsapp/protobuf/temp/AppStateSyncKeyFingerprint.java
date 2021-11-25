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
public class AppStateSyncKeyFingerprint {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> deviceIndexes;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint32")
  private int currentIndex;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int rawId;
}
