package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.InteractiveAnnotation;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

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
  private long fileLength;

  /**
   * The length in seconds of the video that this message wraps
   */
  @ProtobufProperty(index = 5, type = UINT32)
  private int seconds;

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
  private int height;

  /**
   * The unsigned width of the decoded video that this object wraps
   */
  @ProtobufProperty(index = 10, type = UINT32)
  private int width;

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
  private long mediaKeyTimestamp;

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
   * @param media       the non-null video that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param width       the width of the video that the new message wraps
   * @param height      the height of the video that the new message wraps
   * @param seconds     the length in seconds of the video that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleVideoMessageBuilder", builderMethodName = "newVideoMessage", buildMethodName = "create")
  private static VideoMessage videoBuilder(byte @NonNull [] media, String mimeType, String caption, int width, int height, int seconds, ContextInfo contextInfo) {
    /*
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.VIDEO);
    return VideoMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().toByteArray())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.VIDEO.defaultMimeType()))
            .caption(caption)
            .width(width)
            .height(height)
            .seconds(seconds)
            .contextInfo(contextInfo)
            .create();
    */
    throw new UnsupportedOperationException("Work in progress");
  }

  /**
   * Constructs a new builder to create a VideoMessage that wraps a video that will be played as a gif.
   * Wrapping a gif file instead of a video will result in an exception if detected or in an unplayable message.
   * This is because Whatsapp doesn't support standard gifs.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media       the non-null video that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param width       the width of the video that the new message wraps
   * @param height      the height of the video that the new message wraps
   * @param gifAttribution     the length in seconds of the video that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleGifBuilder", builderMethodName = "newGifMessage", buildMethodName = "create")
  private static VideoMessage gifBuilder(byte @NonNull [] media, String mimeType, String caption, int width, int height, VideoMessageAttribution gifAttribution, ContextInfo contextInfo) {
/*
    Validate.isTrue(!Objects.equals(guessMimeType(media), "image/gif") && !Objects.equals(mimeType, "image/gif"), "Cannot create a VideoMessage with mime type image/gif: gif messages on whatsapp are videos played as gifs");
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.VIDEO);
    return VideoMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().toByteArray())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.VIDEO.defaultMimeType()))
            .caption(caption)
            .width(width)
            .height(height)
            .gifPlayback(true)
            .gifAttribution(Optional.ofNullable(gifAttribution).orElse(VideoMessageAttribution.NONE))
            .caption(caption)
            .contextInfo(contextInfo)
            .create();
 */
    throw new UnsupportedOperationException("Work in progress");
  }

  private static String guessMimeType(byte[] media) {
    try {
      return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media));
    } catch (Throwable ignored) {
      return "application/octet-stream";
    }
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
