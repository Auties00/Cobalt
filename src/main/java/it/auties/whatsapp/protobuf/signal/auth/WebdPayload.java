package it.auties.whatsapp.protobuf.signal.auth;

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
public class WebdPayload {
  @JsonProperty(value = "11")
  @JsonPropertyDescription("bytes")
  private byte[] features;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("string")
  private String documentTypes;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EDocument;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EAudio;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EVideo;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EImage;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("bool")
  private boolean supportsMediaRetry;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bool")
  private boolean supportsUrlMessages;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bool")
  private boolean supportsDocumentMessages;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bool")
  private boolean supportsStarredMessages;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bool")
  private boolean usesParticipantInKey;
}
