package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
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

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
@Accessors(fluent = true)
public final class VideoMessage extends MediaMessage {
  /**
   * The upload url of the encoded video that this object wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String url;

  /**
   * The mime type of the video that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String mimetype;

  /**
   * The sha256 of the decoded video that this object wraps
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded video that this object wraps
   */
  @JsonProperty("4")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  /**
   * The length in seconds of the video that this message wraps
   */
  @JsonProperty("5")
  @JsonPropertyDescription("uint32")
  private int seconds;

  /**
   * The media key of the video that this object wraps.
   */
  @JsonProperty("6")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  /**
   * The caption, that is the text below the video, of this video message
   */
  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String caption;

  /**
   * Determines whether this object wraps a video that must be played as a gif
   */
  @JsonProperty("8")
  @JsonPropertyDescription("bool")
  private boolean gifPlayback;

  /**
   * The unsigned height of the decoded video that this object wraps
   */
  @JsonProperty("9")
  @JsonPropertyDescription("uint32")
  private int height;

  /**
   * The unsigned width of the decoded video that this object wraps
   */
  @JsonProperty("10")
  @JsonPropertyDescription("uint32")
  private int width;

  /**
   * The sha256 of the encoded video that this object wraps
   */
  @JsonProperty("11")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  /**
   * Interactive annotations
   */
  @JsonProperty("12")
  @JsonPropertyDescription("interactiveAnnotations")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  /**
   * The direct path to the encoded image that this object wraps
   */
  @JsonProperty("13")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link VideoMessage#key()}
   */
  @JsonProperty("14")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  /**
   * The thumbnail for this video message encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * The sidecar for the decoded video that this message wraps
   */
  @JsonProperty("18")
  @JsonPropertyDescription("bytes")
  private byte[] streamingSidecar;

  /**
   * The source from where the gif that this message wraps comes from.
   * This property is defined only if {@link VideoMessage#gifPlayback}.
   */
  @JsonProperty("19")
  @JsonPropertyDescription("gifAttribution")
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
  @Builder(builderClassName = "NewVideoMessageBuilder", builderMethodName = "newVideoMessage", buildMethodName = "create")
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
  @Builder(builderClassName = "NewGifMessageBuilder", builderMethodName = "newGifMessage", buildMethodName = "create")
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

  private static @NonNull String guessMimeType(byte[] media) {
    try {
      return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media));
    } catch (Throwable ignored) {
      return "application/octet-stream";
    }
  }

  private static VideoMessageBuilder<?, ?> builder(){
    return new VideoMessageBuilderImpl();
  }

  /**
   * Returns the media type of the video that this object wraps
   *
   * @return {@link MediaMessageType#VIDEO}
   */
  @Override
  public @NonNull MediaMessageType type() {
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
}
