package it.auties.whatsapp.model.message.standard;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;
import static it.auties.whatsapp.model.message.model.MediaMessageType.STICKER;
import static it.auties.whatsapp.util.Medias.Format.PNG;
import static java.util.Objects.requireNonNullElse;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message holding a sticker inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
public final class StickerMessage extends MediaMessage {
  /**
   * The upload url of the encoded sticker that this object wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String mediaUrl;

  /**
   * The sha256 of the decoded sticker that this object wraps
   */
  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] mediaSha256;

  /**
   * The sha256 of the encoded sticker that this object wraps
   */
  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] mediaEncryptedSha256;

  /**
   * The media key of the sticker that this object wraps
   */
  @ProtobufProperty(index = 4, type = BYTES)
  private byte[] mediaKey;

  /**
   * The mime type of the sticker that this object wraps. Most of the seconds this is
   * {@link MediaMessageType#defaultMimeType()}
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String mimetype;

  /**
   * The unsigned height of the decoded sticker that this object wraps
   */
  @ProtobufProperty(index = 6, type = UINT32)
  private Integer height;

  /**
   * The unsigned width of the decoded sticker that this object wraps
   */
  @ProtobufProperty(index = 7, type = UINT32)
  private Integer width;

  /**
   * The direct path to the encoded sticker that this object wraps
   */
  @ProtobufProperty(index = 8, type = STRING)
  private String mediaDirectPath;

  /**
   * The unsigned size of the decoded sticker that this object wraps
   */
  @ProtobufProperty(index = 9, type = UINT64)
  private long mediaSize;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for
   * {@link StickerMessage#mediaKey()}
   */
  @ProtobufProperty(index = 10, type = UINT64)
  private long mediaKeyTimestamp;

  /**
   * The codeLength of the first frame
   */
  @ProtobufProperty(index = 11, type = UINT32)
  private Integer firstFrameLength;

  /**
   * The sidecar for the first frame
   */
  @ProtobufProperty(index = 12, type = BYTES)
  private byte[] firstFrameSidecar;

  /**
   * Determines whether this sticker message is animated
   */
  @ProtobufProperty(index = 13, type = BOOL)
  private boolean animated;

  /**
   * The thumbnail for this sticker message encoded as png in an array of bytes
   */
  @ProtobufProperty(index = 16, type = BYTES)
  private byte[] thumbnail;

  @ProtobufProperty(index = 18, name = "stickerSentTs", type = INT64)
  private long stickerSentTimestamp;

  @ProtobufProperty(index = 19, name = "isAvatar", type = BOOL)
  private boolean isAvatar;

  /**
   * Constructs a new builder to create a StickerMessage. The result can be later sent using
   * {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param mediaConnection the media connection to use to upload this message
   * @param media           the non-null sticker that the new message wraps
   * @param mimeType        the mime type of the new message, by default
   *                        {@link MediaMessageType#defaultMimeType()}
   * @param thumbnail       the thumbnail of the sticker that the new message wraps as a png
   * @param animated        whether the sticker that the new message wraps is animated
   * @param contextInfo     the context info that the new message wraps
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleStickerMessageBuilder", builderMethodName = "simpleBuilder")
  private static StickerMessage customBuilder(byte[] media, String mimeType, byte[] thumbnail,
      boolean animated, ContextInfo contextInfo) {
    return StickerMessage.builder().decodedMedia(media).mediaKeyTimestamp(Clock.nowInSeconds())
        .mimetype(requireNonNullElse(mimeType, STICKER.defaultMimeType()))
        .thumbnail(thumbnail != null ? thumbnail : Medias.getThumbnail(media, PNG).orElse(null))
        .animated(animated)
        .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::new)).build();
  }

  /**
   * Returns the media type of the sticker that this object wraps
   *
   * @return {@link MediaMessageType#STICKER}
   */
  @Override
  public MediaMessageType mediaType() {
    return MediaMessageType.STICKER;
  }
}