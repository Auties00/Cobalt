package it.auties.whatsapp.protobuf.model.recent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class RecentStickerWeight {
  @JsonProperty(value = "2")
  private float weight;

  @JsonProperty(value = "1")
  private String filehash;
}
