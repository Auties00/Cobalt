package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.InteractiveAnnotation;
import it.auties.whatsapp.protobuf.message.model.MediaMessage;
import it.auties.whatsapp.protobuf.message.model.MediaMessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawImageMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class ImageMessage extends MediaMessage {
  /**
   * The upload url of the encoded image that this object wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String url;

  /**
   * The mime type of the image that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String mimetype;

  /**
   * The caption of this message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String caption;

  /**
   * The sha256 of the decoded image that this object wraps
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded image that this object wraps
   */
  @JsonProperty("5")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  /**
   * The unsigned height of the decoded image that this object wraps
   */
  @JsonProperty("6")
  @JsonPropertyDescription("uint32")
  private int height;

  /**
   * The unsigned width of the decoded image that this object wraps
   */
  @JsonProperty("7")
  @JsonPropertyDescription("uint32")
  private int width;

  /**
   * The media key of the image that this object wraps
   */
  @JsonProperty("8")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  /**
   * The sha256 of the encoded image that this object wraps
   */
  @JsonProperty("9")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  /**
   * Interactive annotations
   */
  @JsonProperty("10")
  @JsonPropertyDescription("annotations")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;
  
  /**
   * The direct path to the encoded image that this object wraps
   */
  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link ImageMessage#key()}
   */
  @JsonProperty("12")
  @JsonPropertyDescription("uint64")
  private long mediaKeyTimestamp;
  
  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * The sidecar for the first sidecar
   */
  @JsonProperty("18")
  @JsonPropertyDescription("bytes")
  private byte[] firstScanSidecar;

  /**
   * The length of the first scan
   */
  @JsonProperty("19")
  @JsonPropertyDescription("uint32")
  private int firstScanLength;

  /**
   * Experiment Group Id
   */
  @JsonProperty("20")
  @JsonPropertyDescription("uint32")
  private int experimentGroupId;
  
  /**
   * The sidecar for the scans of the decoded image
   */
  @JsonProperty("21")
  @JsonPropertyDescription("bytes")
  private byte[] scansSidecar;

  /**
   * The length of each scan of the decoded image
   */
  @JsonProperty("22")
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> scanLengths;

  /**
   * The sha256 of the decoded image in medium quality
   */
  @JsonProperty("23")
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileSha256;

  /**
   * The sha256 of the encoded image in medium quality
   */
  @JsonProperty("24")
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileEncSha256;
  
  /**
   * Constructs a new builder to create a ImageMessage.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media       the non-null image that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param width       the width of the image that the new message wraps
   * @param height      the height of the image that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "NewImageMessageBuilder", builderMethodName = "newImageMessage", buildMethodName = "create")
  private static ImageMessage simpleBuilder(byte @NonNull [] media, String mimeType, String caption, int width, int height, ContextInfo contextInfo) {
    /*
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.IMAGE);
    return ImageMessage.newRawImageMessage()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().toByteArray())
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
     */

    throw new UnsupportedOperationException("Work in progress");
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
