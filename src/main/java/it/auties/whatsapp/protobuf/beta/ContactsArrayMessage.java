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
public class ContactsArrayMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("ContactMessage")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ContactMessage> contacts;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String displayName;
}
