package it.auties.whatsapp.model.info;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that holds the information related to an advertisement.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ExternalAdReplyInfo implements Info {
  /**
   * The title of this advertisement
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  /**
   * The body of this advertisement
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String body;

  /**
   * The media type of this ad, if any is specified
   */
  @ProtobufProperty(index = 3, type = MESSAGE, implementation = ExternalAdReplyInfo.ExternalAdReplyInfoMediaType.class)
  private ExternalAdReplyInfoMediaType mediaType;

  /**
   * The url of the thumbnail for the media of this ad, if any is specified
   */
  @ProtobufProperty(index = 4, type = STRING)
  private String thumbnailUrl;

  /**
   * The url of the media of this ad, if any is specified
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String mediaUrl;

  /**
   * The thumbnail for the media of this ad, if any is specified
   */
  @ProtobufProperty(index = 6, type = BYTES)
  private byte[] thumbnail;

  /**
   * The source type of this ad
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String sourceType;

  /**
   * The source jid of this ad
   */
  @ProtobufProperty(index = 8, type = STRING)
  private String sourceId;

  /**
   * The source url of this ad
   */
  @ProtobufProperty(index = 9, type = STRING)
  private String sourceUrl;

  @ProtobufProperty(index = 10, name = "containsAutoReply", type = ProtobufType.BOOL)
  private Boolean containsAutoReply;

  @ProtobufProperty(index = 11, name = "renderLargerThumbnail", type = ProtobufType.BOOL)
  private Boolean renderLargerThumbnail;

  @ProtobufProperty(index = 12, name = "showAdAttribution", type = ProtobufType.BOOL)
  private Boolean showAdAttribution;

  @ProtobufProperty(index = 13, name = "ctwaClid", type = ProtobufType.STRING)
  private String ctwaClid;

  /**
   * The constants of this enumerated type describe the various types of media that an ad can wrap
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("MediaType")
  public enum ExternalAdReplyInfoMediaType {

    /**
     * No media
     */
    NONE(0),
    /**
     * Image
     */
    IMAGE(1),
    /**
     * Video
     */
    VIDEO(2);
    @Getter
    private final int index;

    @JsonCreator
    public static ExternalAdReplyInfoMediaType of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }
}