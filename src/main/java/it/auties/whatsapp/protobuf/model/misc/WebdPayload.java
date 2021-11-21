package it.auties.whatsapp.protobuf.model.misc;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  private byte[] features;

  @JsonProperty(value = "10")
  private String documentTypes;

  @JsonProperty(value = "9")
  private boolean supportsE2EDocument;

  @JsonProperty(value = "8")
  private boolean supportsE2EAudio;

  @JsonProperty(value = "7")
  private boolean supportsE2EVideo;

  @JsonProperty(value = "6")
  private boolean supportsE2EImage;

  @JsonProperty(value = "5")
  private boolean supportsMediaRetry;

  @JsonProperty(value = "4")
  private boolean supportsUrlMessages;

  @JsonProperty(value = "3")
  private boolean supportsDocumentMessages;

  @JsonProperty(value = "2")
  private boolean supportsStarredMessages;

  @JsonProperty(value = "1")
  private boolean usesParticipantInKey;
}
