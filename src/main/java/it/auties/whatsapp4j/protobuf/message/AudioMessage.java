package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an audio inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public class AudioMessage extends MediaMessage implements ContextualMessage {
  @JsonProperty(value = "18")
  private byte[] streamingSidecar;

  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "10")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "9")
  private String directPath;

  @JsonProperty(value = "8")
  private byte[] fileEncSha256;

  @JsonProperty(value = "7")
  private byte[] mediaKey;

  @JsonProperty(value = "6")
  private boolean ptt;

  @JsonProperty(value = "5")
  private int seconds;

  @JsonProperty(value = "4")
  private long fileLength;

  @JsonProperty(value = "3")
  private byte[] fileSha256;

  @JsonProperty(value = "2")
  private String mimetype;

  @JsonProperty(value = "1")
  private String url;

  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.AUDIO;
  }
}
