package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import it.auties.whatsapp4j.protobuf.model.recent.RecentEmojiWeight;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class RecentEmojiWeightsAction {
  @JsonProperty(value = "1")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<RecentEmojiWeight> weights;
}
