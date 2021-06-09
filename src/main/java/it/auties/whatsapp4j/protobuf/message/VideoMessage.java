package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.model.InteractiveAnnotation;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class VideoMessage extends MediaMessage {
  /**
   * The source from where the gif that this message wraps comes from.
   * This property is defined only if {@link VideoMessage#gifPlayback}.
   */
  @JsonProperty(value = "19")
  private VideoMessageAttribution gifAttribution;

  /**
   * The sidecar for the decoded video that this message wraps
   */
  @JsonProperty(value = "18")
  private byte[] streamingSidecar;

  /**
   * The thumbnail for this video message encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link VideoMessage#mediaKey()}
   */
  @JsonProperty(value = "14")
  private long mediaKeyTimestamp;

  /**
   * The direct path to the encoded image that this object wraps
   */
  @JsonProperty(value = "13")
  private String directPath;

  /**
   * Interactive annotations
   */
  @JsonProperty(value = "12")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  /**
   * The sha256 of the encoded video that this object wraps
   */
  @JsonProperty(value = "11")
  private byte[] fileEncSha256;

  /**
   * The unsigned width of the decoded video that this object wraps
   */
  @JsonProperty(value = "10")
  private int width;

  /**
   * The unsigned height of the decoded video that this object wraps
   */
  @JsonProperty(value = "9")
  private int height;

  /**
   * Determines whether this object wraps a video that must be played as a gif
   */
  @JsonProperty(value = "8")
  private boolean gifPlayback;

  /**
   * The caption, that is the text below the video, of this video message
   */
  @JsonProperty(value = "7")
  private String caption;

  /**
   * The media key of the video that this object wraps.
   * This key is used to decrypt the encoded media by {@link CypherUtils#mediaDecrypt(MediaMessage)}
   */
  @JsonProperty(value = "6")
  private byte[] mediaKey;

  /**
   * The length in seconds of the video that this message wraps
   */
  @JsonProperty(value = "5")
  private int seconds;

  /**
   * The unsigned size of the decoded video that this object wraps
   */
  @JsonProperty(value = "4")
  private long fileLength;

  /**
   * The sha256 of the decoded video that this object wraps
   */
  @JsonProperty(value = "3")
  private byte[] fileSha256;

  /**
   * The mime type of the video that this object wraps.
   * Most of the times this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty(value = "2")
  private String mimetype;

  /**
   * The upload url of the encoded video that this object wraps
   */
  @JsonProperty(value = "1")
  private String url;

  /**
   * Returns the media type of the video that this object wraps
   *
   * @return {@link MediaMessageType#VIDEO}
   */
  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.VIDEO;
  }

  /**
   * The constants of this enumerated type describe the various sources from where a gif can come from
   */
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

    private final @Getter int index;

    VideoMessageAttribution(int index) {
      this.index = index;
    }

    @JsonCreator
    public static VideoMessageAttribution forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
