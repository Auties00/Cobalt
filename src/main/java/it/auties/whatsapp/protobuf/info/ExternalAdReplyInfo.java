package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that holds the information related to an advertisement.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ExternalAdReplyInfo {
  /**
   * The title of this advertisement
   */
  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String title;

  /**
   * The body of this advertisement
   */
  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String body;

  /**
   * The media type of this ad, if any is specified
   */
  @JsonProperty(value = "3")
  @JsonPropertyDescription("ExternalAdReplyInfoMediaType")
  private ExternalAdReplyInfoMediaType mediaType;

  /**
   * The url of the thumbnail for the media of this ad, if any is specified
   */
  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String thumbnailUrl;

  /**
   * The url of the media of this ad, if any is specified
   */
  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String mediaUrl;

  /**
   * The thumbnail for the media of this ad, if any is specified
   */
  @JsonProperty(value = "6")
  @JsonPropertyDescription("string")
  private byte[] thumbnail;

  /**
   * The source type of this ad
   */
  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String sourceType;

  /**
   * The source id of this ad
   */
  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String sourceId;

  /**
   * The source url of this ad
   */
  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String sourceUrl;

  /**
   * The constants of this enumerated type describe the various types of media that an ad can wrap
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
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

    private final @Getter int index;

    @JsonCreator
    public static ExternalAdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
