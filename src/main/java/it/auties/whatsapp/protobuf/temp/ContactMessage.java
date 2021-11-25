package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class ContactMessage {
  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("string")
  private String vcard;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String displayName;
}
