package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a sticker inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class StickerMessage extends MediaMessage {
  /**
   * The thumbnail for this sticker message encoded as png in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] pngThumbnail;

  /**
   * Determines whether this sticker message is animated
   */
  @JsonProperty(value = "13")
  private boolean isAnimated;

  /**
   * The sidecar for the first frame
   */
  @JsonProperty(value = "12")
  private byte[] firstFrameSidecar;

  /**
   * The length of the first frame
   */
  @JsonProperty(value = "11")
  private int firstFrameLength;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link StickerMessage#mediaKey()}
   */
  @JsonProperty(value = "10")
  private long mediaKeyTimestamp;

  /**
   * The unsigned size of the decoded sticker that this object wraps
   */
  @JsonProperty(value = "9")
  private long fileLength;

  /**
   * The direct path to the encoded sticker that this object wraps
   */
  @JsonProperty(value = "8")
  private String directPath;

  /**
   * The unsigned width of the decoded sticker that this object wraps
   */
  @JsonProperty(value = "7")
  private int width;

  /**
   * The unsigned height of the decoded sticker that this object wraps
   */
  @JsonProperty(value = "6")
  private int height;

  /**
   * The mime type of the sticker that this object wraps.
   * Most of the times this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty(value = "5")
  private String mimetype;

  /**
   * The 
   */
  @JsonProperty(value = "4")
  private byte[] mediaKey;

  /**
   * The sha256 of the encoded sticker that this object wraps
   */
  @JsonProperty(value = "3")
  private byte[] fileEncSha256;

  /**
   * The sha256 of the decoded sticker that this object wraps
   */
  @JsonProperty(value = "2")
  private byte[] fileSha256;

  /**
   * The upload url of the encoded sticker that this object wraps
   */
  @JsonProperty(value = "1")
  private String url;

  @Override
  public MediaMessageType type() {
    return MediaMessageType.STICKER;
  }
}
