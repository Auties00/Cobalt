package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a sticker inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public final class StickerMessage extends MediaMessage {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] pngThumbnail;

  @JsonProperty(value = "13")
  private boolean isAnimated;

  @JsonProperty(value = "12")
  private byte[] firstFrameSidecar;

  @JsonProperty(value = "11")
  private int firstFrameLength;

  @JsonProperty(value = "10")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "9")
  private long fileLength;

  @JsonProperty(value = "8")
  private String directPath;

  @JsonProperty(value = "7")
  private int width;

  @JsonProperty(value = "6")
  private int height;

  @JsonProperty(value = "5")
  private String mimetype;

  @JsonProperty(value = "4")
  private byte[] mediaKey;

  @JsonProperty(value = "3")
  private byte[] fileEncSha256;

  @JsonProperty(value = "2")
  private byte[] fileSha256;

  @JsonProperty(value = "1")
  private String url;

  @Override
  public MediaMessageType type() {
    return MediaMessageType.STICKER;
  }
}
