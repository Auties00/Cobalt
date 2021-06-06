package it.auties.whatsapp4j.protobuf.button;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HydratedURLButton {
  @JsonProperty(value = "2")
  private String url;

  @JsonProperty(value = "1")
  private String displayText;
}
