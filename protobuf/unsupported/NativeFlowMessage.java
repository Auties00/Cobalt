package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class NativeFlowMessage {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int32")
  private int messageVersion;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String messageParamsJson;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("NativeFlowButton")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<NativeFlowButton> buttons;
}
