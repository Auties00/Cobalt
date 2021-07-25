package it.auties.whatsapp4j.protobuf.model.recent;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class RecentEmojiWeight {
  @JsonProperty(value = "2")
  private float weight;

  @JsonProperty(value = "1")
  private String emoji;
}
