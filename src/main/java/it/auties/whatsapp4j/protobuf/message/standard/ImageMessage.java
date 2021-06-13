package it.auties.whatsapp4j.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessage;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessageType;
import it.auties.whatsapp4j.protobuf.model.InteractiveAnnotation;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import lombok.NonNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawImageMessage", buildMethodName = "create")
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
   * Constructs a new builder to create a ImageMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param media       the non null image that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param width       the width of the image that the new message wraps
   * @param height      the height of the image that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non null new message
   */
  @Builder(builderClassName = "NewImageMessageBuilder", builderMethodName = "newImageMessage", buildMethodName = "create")
  private static ImageMessage simpleBuilder(byte @NonNull [] media, String mimeType, String caption, int width, int height, ContextInfo contextInfo) {
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.IMAGE);
    return ImageMessage.newRawImageMessage()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().data())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.IMAGE.defaultMimeType()))
            .caption(caption)
            .width(width)
            .height(height)
            .contextInfo(contextInfo)
            .create();
  }

  /**
   * Returns the media type of the image that this object wraps
   *
   * @return {@link MediaMessageType#IMAGE}
   */
  @Override
  public @NonNull MediaMessageType type() {
    return MediaMessageType.IMAGE;
  }
}
