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
public class HydratedCallButton {
  @JsonProperty(value = "2")
  private String phoneNumber;

  @JsonProperty(value = "1")
  private String displayText;
}
