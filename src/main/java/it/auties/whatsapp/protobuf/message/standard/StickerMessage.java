package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MediaMessage;
import it.auties.whatsapp.protobuf.message.model.MediaMessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a sticker inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class StickerMessage extends MediaMessage {
  /**
   * The upload url of the encoded sticker that this object wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String url;

  /**
   * The sha256 of the decoded sticker that this object wraps
   */
  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  /**
   * The sha256 of the encoded sticker that this object wraps
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  /**
   * The media key of the sticker that this object wraps
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  /**
   * The mime type of the sticker that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String mimetype;

  /**
   * The unsigned height of the decoded sticker that this object wraps
   */
  @JsonProperty("6")
  @JsonPropertyDescription("uint32")
  private int height;

  /**
   * The unsigned width of the decoded sticker that this object wraps
   */
  @JsonProperty("7")
  @JsonPropertyDescription("uint32")
  private int width;

  /**
   * The direct path to the encoded sticker that this object wraps
   */
  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The unsigned size of the decoded sticker that this object wraps
   */
  @JsonProperty("9")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link StickerMessage#key()}
   */
  @JsonProperty("10")
  @JsonPropertyDescription("uint64")
  private long mediaKeyTimestamp;

  /**
   * The length of the first frame
   */
  @JsonProperty("11")
  @JsonPropertyDescription("uint32")
  private int firstFrameLength;

  /**
   * The sidecar for the first frame
   */
  @JsonProperty("12")
  @JsonPropertyDescription("bytes")
  private byte[] firstFrameSidecar;

  /**
   * Determines whether this sticker message is animated
   */
  @JsonProperty("13")
  @JsonPropertyDescription("bool")
  private boolean animated;

  /**
   * The thumbnail for this sticker message encoded as png in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * Constructs a new builder to create a StickerMessage.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media        the non-null sticker that the new message wraps
   * @param mimeType     the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param pngThumbnail the thumbnail of the sticker that the new message wraps as a png
   * @param isAnimated   whether the sticker that the new message wraps is animated
   * @param contextInfo  the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "NewStickerMessageBuilder", builderMethodName = "newStickerMessage", buildMethodName = "create")
  private static StickerMessage builder(byte @NonNull [] media, String mimeType, byte[] pngThumbnail, boolean isAnimated, ContextInfo contextInfo) {
    /*
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.STICKER);
    return StickerMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().data())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.STICKER.defaultMimeType()))
            .firstFrameSidecar(upload.sidecar())
            .firstFrameLength(upload.sidecar().length)
            .isAnimated(isAnimated)
            .pngThumbnail(pngThumbnail)
            .contextInfo(contextInfo)
            .create();
     */
    throw new UnsupportedOperationException("Work in progress");
  }

  private static StickerMessageBuilder<?, ?> builder(){
    return new StickerMessageBuilderImpl();
  }
  
  /**
   * Returns the media type of the sticker that this object wraps
   *
   * @return {@link MediaMessageType#STICKER}
   */
  @Override
  public @NonNull MediaMessageType type() {
    return MediaMessageType.STICKER;
  }
}
