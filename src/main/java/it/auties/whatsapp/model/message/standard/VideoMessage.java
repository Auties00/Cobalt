package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaDimensions;
import it.auties.whatsapp.model.message.model.InteractiveAnnotation;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.*;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;
import static it.auties.whatsapp.model.message.model.MediaMessageType.VIDEO;
import static it.auties.whatsapp.util.Medias.Format.JPG;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawVideoMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class VideoMessage extends MediaMessage {
  /**
   * The upload url of the encoded video that this object wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String url;

  /**
   * The mime type of the video that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String mimetype;

  /**
   * The sha256 of the decoded video that this object wraps
   */
  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded video that this object wraps
   */
  @ProtobufProperty(index = 4, type = UINT64)
  private Long fileLength;

  /**
   * The length in seconds of the video that this message wraps
   */
  @ProtobufProperty(index = 5, type = UINT32)
  private Integer duration;

  /**
   * The media key of the video that this object wraps.
   */
  @ProtobufProperty(index = 6, type = BYTES)
  private byte[] key; 

  /**
   * The caption, that is the text below the video, of this video message
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String caption;

  /**
   * Determines whether this object wraps a video that must be played as a gif
   */
  @ProtobufProperty(index = 8, type = BOOLEAN)
  private boolean gifPlayback;

  /**
   * The unsigned height of the decoded video that this object wraps
   */
  @ProtobufProperty(index = 9, type = UINT32)
  private Integer height;

  /**
   * The unsigned width of the decoded video that this object wraps
   */
  @ProtobufProperty(index = 10, type = UINT32)
  private Integer width;

  /**
   * The sha256 of the encoded video that this object wraps
   */
  @ProtobufProperty(index = 11, type = BYTES)
  private byte[] fileEncSha256;

  /**
   * Interactive annotations
   */
  @ProtobufProperty(index = 12, type = MESSAGE,
          concreteType = InteractiveAnnotation.class, repeated = true)
  private List<InteractiveAnnotation> interactiveAnnotations;

  /**
   * The direct path to the encoded image that this object wraps
   */
  @ProtobufProperty(index = 13, type = STRING)
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link VideoMessage#key()}
   */
  @ProtobufProperty(index = 14, type = INT64)
  private Long mediaKeyTimestamp;

  /**
   * The thumbnail for this video message encoded as jpeg in an array of bytes
   */
  @ProtobufProperty(index = 16, type = BYTES)
  private byte[] thumbnail;

  /**
   * The sidecar for the decoded video that this message wraps
   */
  @ProtobufProperty(index = 18, type = BYTES)
  private byte[] streamingSidecar;

  /**
   * The source from where the gif that this message wraps comes from.
   * This property is defined only if {@link VideoMessage#gifPlayback}.
   */
  @ProtobufProperty(index = 19, type = MESSAGE, concreteType = VideoMessageAttribution.class)
  private VideoMessageAttribution gifAttribution;

  /**
   * Constructs a new builder to create a VideoMessage that wraps a video.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param storeId     the id of the store where this message will be stored
   * @param media       the non-null video that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param thumbnail   the thumbnail of the sticker that the new message wraps as a jpg
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleVideoMessageBuilder", builderMethodName = "newVideoMessage", buildMethodName = "create")
  private static VideoMessage videoBuilder(int storeId, byte @NonNull [] media, String mimeType, String caption, byte[] thumbnail, ContextInfo contextInfo) {
    var store = WhatsappStore.findStoreById(storeId)
            .orElseThrow(() -> new NoSuchElementException("Cannot create video message, invalid store id: %s".formatted(storeId)));
    var dimensions = Medias.getDimensions(media, true);
    var duration = Medias.getDuration(media, true);
    var upload = Medias.upload(media, VIDEO, store);
    return VideoMessage.newRawVideoMessage()
            .storeId(storeId)
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .key(upload.mediaKey())
            .mediaKeyTimestamp(Clock.now())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(upload.fileLength())
            .mimetype(requireNonNullElse(mimeType, VIDEO.defaultMimeType()))
            .thumbnail(thumbnail != null ? thumbnail : Medias.getThumbnail(media, Medias.Format.VIDEO))
            .caption(caption)
            .width(dimensions.width())
            .height(dimensions.height())
            .duration(duration)
            .contextInfo(contextInfo)
            .create();
  }

  /**
   * Constructs a new builder to create a VideoMessage that wraps a video that will be played as a gif.
   * Wrapping a gif file instead of a video will result in an exception if detected or in an unplayable message.
   * This is because Whatsapp doesn't support standard gifs.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param storeId     the id of the store where this message will be stored
   * @param media       the non-null video that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param gifAttribution     the length in seconds of the video that the new message wraps
   * @param thumbnail   the thumbnail of the sticker that the new message wraps as a jpg
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleGifBuilder", builderMethodName = "newGifMessage", buildMethodName = "create")
  private static VideoMessage gifBuilder(int storeId, byte @NonNull [] media, String mimeType, String caption, VideoMessageAttribution gifAttribution, byte[] thumbnail, ContextInfo contextInfo) {
    Validate.isTrue(isGif(media, mimeType), "Cannot create a VideoMessage with mime type image/gif: gif messages on whatsapp are videos played as gifs");
    var store = WhatsappStore.findStoreById(storeId)
            .orElseThrow(() -> new NoSuchElementException("Cannot create video message, invalid store id: %s".formatted(storeId)));
    var dimensions = Medias.getDimensions(media, true);
    var duration = Medias.getDuration(media, true);
    var upload = Medias.upload(media, VIDEO, store);
    return VideoMessage.newRawVideoMessage()
            .storeId(storeId)
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .key(upload.mediaKey())
            .mediaKeyTimestamp(Clock.now())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(upload.fileLength())
            .mimetype(requireNonNullElse(mimeType, VIDEO.defaultMimeType()))
            .thumbnail(thumbnail != null ? thumbnail : Medias.getThumbnail(media, Medias.Format.VIDEO))
            .caption(caption)
            .width(dimensions.width())
            .height(dimensions.height())
            .duration(duration)
            .gifPlayback(true)
            .gifAttribution(requireNonNullElse(gifAttribution, VideoMessageAttribution.NONE))
            .contextInfo(contextInfo)
            .create();
  }

  private static boolean isGif(byte[] media, String mimeType) {
    return Medias.getMimeType(media).filter("image/gif"::equals).isEmpty()
            && !Objects.equals(mimeType, "image/gif");
  }

  /**
   * Returns the media type of the video that this object wraps
   *
   * @return {@link MediaMessageType#VIDEO}
   */
  @Override
  public MediaMessageType type() {
    return MediaMessageType.VIDEO;
  }

  /**
   * The constants of this enumerated type describe the various sources from where a gif can come from
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum VideoMessageAttribution {
    /**
     * No source was specified
     */
    NONE(0),

    /**
     * Giphy
     */
    GIPHY(1),

    /**
     * Tenor
     */
    TENOR(2);

    @Getter
    private final int index;

    @JsonCreator
    public static VideoMessageAttribution forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  public static abstract class VideoMessageBuilder<C extends VideoMessage, B extends VideoMessageBuilder<C, B>> extends MediaMessageBuilder<C, B> {
    public B interactiveAnnotations(List<InteractiveAnnotation> interactiveAnnotations) {
      if(this.interactiveAnnotations == null) this.interactiveAnnotations = new ArrayList<>();
      this.interactiveAnnotations.addAll(interactiveAnnotations);
      return self();
    }
  }
}
