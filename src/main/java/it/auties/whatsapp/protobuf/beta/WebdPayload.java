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
public class WebdPayload {

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] features;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("string")
  private String documentTypes;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsE2EDocument;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsE2EAudio;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsE2EVideo;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsE2EImage;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsMediaRetry;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsUrlMessages;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsDocumentMessages;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bool")
  private boolean supportsStarredMessages;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bool")
  private boolean usesParticipantInKey;
}
