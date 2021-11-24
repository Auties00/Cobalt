package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class TemplateButtonReplyMessage {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int selectedIndex;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String selectedDisplayText;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String selectedId;
}
