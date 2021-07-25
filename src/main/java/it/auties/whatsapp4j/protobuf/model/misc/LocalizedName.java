package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
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
