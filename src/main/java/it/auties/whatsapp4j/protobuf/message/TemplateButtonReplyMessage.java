package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class TemplateButtonReplyMessage implements Message {
  @JsonProperty(value = "4")
  private int selectedIndex;

  @JsonProperty(value = "3")
  private ContextInfo contextInfo;

  @JsonProperty(value = "2")
  private String selectedDisplayText;

  @JsonProperty(value = "1")
  private String selectedId;
}
