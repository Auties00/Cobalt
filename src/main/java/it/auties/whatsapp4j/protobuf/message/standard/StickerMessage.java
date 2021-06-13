package it.auties.whatsapp4j.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessage;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessageType;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a sticker inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
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
   * The media key of the sticker that this object wraps.
   * This key is used to decrypt the encoded media by {@link CypherUtils#mediaDecrypt(MediaMessage)}
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

  /**
   * Constructs a new builder to create a StickerMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param media        the non null sticker that the new message wraps
   * @param mimeType     the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param pngThumbnail the thumbnail of the sticker that the new message wraps as a png
   * @param isAnimated   whether the sticker that the new message wraps is animated
   * @param contextInfo  the context info that the new message wraps
   *
   * @return a non null new message
   */
  @Builder(builderClassName = "NewStickerMessageBuilder", builderMethodName = "newStickerMessage", buildMethodName = "create")
  private static StickerMessage builder(byte @NotNull [] media, String mimeType, byte[] pngThumbnail, boolean isAnimated, ContextInfo contextInfo) {
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
  public MediaMessageType type() {
    return MediaMessageType.STICKER;
  }
}
