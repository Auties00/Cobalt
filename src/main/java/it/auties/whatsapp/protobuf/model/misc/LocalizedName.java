package it.auties.whatsapp.protobuf.model.misc;

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
public class LocalizedName {
  @JsonProperty(value = "3")
  private String verifiedName;

  @JsonProperty(value = "2")
  private String lc;

  @JsonProperty(value = "1")
  private String lg;
}
