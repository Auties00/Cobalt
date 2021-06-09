package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.model.InteractiveAnnotation;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class ImageMessage extends MediaMessage {
  /**
   * The sha256 of the encoded image in medium quality
   */
  @JsonProperty(value = "24")
  private byte[] midQualityFileEncSha256;

  /**
   * The sha256 of the decoded image in medium quality
   */
  @JsonProperty(value = "23")
  private byte[] midQualityFileSha256;

  /**
   * The length of each scan of the decoded image
   */
  @JsonProperty(value = "22")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> scanLengths;

  /**
   * The sidecar for the scans of the decoded image
   */
  @JsonProperty(value = "21")
  private byte[] scansSidecar;

  /**
   * The length of the first scan
   */
  @JsonProperty(value = "19")
  private int firstScanLength;

  /**
   * The sidecar for the first sidecar
   */
  @JsonProperty(value = "18")
  private byte[] firstScanSidecar;

  /**
   * Experiment Group Id
   */
  @JsonProperty(value = "20")
  private int experimentGroupId;

  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link ImageMessage#mediaKey()}
   */
  @JsonProperty(value = "12")
  private long mediaKeyTimestamp;

  /**
   * The direct path to the encoded image that this object wraps
   */
  @JsonProperty(value = "11")
  private String directPath;

  /**
   * Interactive annotations
   */
  @JsonProperty(value = "10")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  /**
   * The sha256 of the encoded image that this object wraps
   */
  @JsonProperty(value = "9")
  private byte[] fileEncSha256;

  /**
   * The media key of the image that this object wraps.
   * This key is used to decrypt the encoded media by {@link CypherUtils#mediaDecrypt(MediaMessage)}
   */
  @JsonProperty(value = "8")
  private byte[] mediaKey;

  /**
   * The unsigned width of the decoded image that this object wraps
   */
  @JsonProperty(value = "7")
  private int width;

  /**
   * The unsigned height of the decoded image that this object wraps
   */
  @JsonProperty(value = "6")
  private int height;

  /**
   * The unsigned size of the decoded image that this object wraps
   */
  @JsonProperty(value = "5")
  private long fileLength;

  /**
   * The sha256 of the decoded image that this object wraps
   */
  @JsonProperty(value = "4")
  private byte[] fileSha256;

  /**
   * The caption of this message
   */
  @JsonProperty(value = "3")
  private String caption;

  /**
   * The mime type of the image that this object wraps.
   * Most of the times this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty(value = "2")
  private String mimetype;

  /**
   * The upload url of the encoded image that this object wraps
   */
  @JsonProperty(value = "1")
  private String url;

  /**
   * Returns the media type of the image that this object wraps
   *
   * @return {@link MediaMessageType#IMAGE}
   */
  @Override
  public MediaMessageType type() {
    return MediaMessageType.IMAGE;
  }
}
