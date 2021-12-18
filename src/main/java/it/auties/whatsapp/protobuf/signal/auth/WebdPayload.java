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
  @JsonProperty("11")
  @JsonPropertyDescription("bytes")
  private byte[] features;

  @JsonProperty("10")
  @JsonPropertyDescription("string")
  private String documentTypes;

  @JsonProperty("9")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EDocument;

  @JsonProperty("8")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EAudio;

  @JsonProperty("7")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EVideo;

  @JsonProperty("6")
  @JsonPropertyDescription("bool")
  private boolean supportsE2EImage;

  @JsonProperty("5")
  @JsonPropertyDescription("bool")
  private boolean supportsMediaRetry;

  @JsonProperty("4")
  @JsonPropertyDescription("bool")
  private boolean supportsUrlMessages;

  @JsonProperty("3")
  @JsonPropertyDescription("bool")
  private boolean supportsDocumentMessages;

  @JsonProperty("2")
  @JsonPropertyDescription("bool")
  private boolean supportsStarredMessages;

  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean usesParticipantInKey;
}