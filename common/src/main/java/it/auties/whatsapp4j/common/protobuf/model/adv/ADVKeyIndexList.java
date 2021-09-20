package it.auties.whatsapp4j.common.protobuf.model.adv;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ADVKeyIndexList {
  @JsonProperty(value = "4")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> validIndexes;

  @JsonProperty(value = "3")
  private int currentIndex;

  @JsonProperty(value = "2")
  private long timestamp;

  @JsonProperty(value = "1")
  private int rawId;
}
