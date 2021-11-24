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
public class ContactMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("string")
  private String vcard;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String displayName;
}
