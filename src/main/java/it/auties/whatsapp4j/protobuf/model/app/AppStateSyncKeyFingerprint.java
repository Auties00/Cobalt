package it.auties.whatsapp4j.protobuf.model.app;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class AppStateSyncKeyFingerprint {
  @JsonProperty(value = "3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> deviceIndexes;

  @JsonProperty(value = "2")
  private int currentIndex;

  @JsonProperty(value = "1")
  private int rawId;
}
