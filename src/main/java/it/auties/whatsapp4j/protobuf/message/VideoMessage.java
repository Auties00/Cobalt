package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.model.InteractiveAnnotation;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public final class VideoMessage extends MediaMessage {
  @JsonProperty(value = "19")
  private VideoMessageAttribution gifAttribution;

  @JsonProperty(value = "18")
  private byte[] streamingSidecar;

  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "14")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "13")
  private String directPath;

  @JsonProperty(value = "12")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "11")
  private byte[] fileEncSha256;

  @JsonProperty(value = "10")
  private int width;

  @JsonProperty(value = "9")
  private int height;

  @JsonProperty(value = "8")
  private boolean gifPlayback;

  /**
   * The caption, that is the text below the video, of this video message
   */
  @JsonProperty(value = "7")
  private String caption;

  @JsonProperty(value = "6")
  private byte[] mediaKey;

  @JsonProperty(value = "5")
  private int seconds;

  @JsonProperty(value = "4")
  private long fileLength;

  @JsonProperty(value = "3")
  private byte[] fileSha256;

  @JsonProperty(value = "2")
  private String mimetype;

  @JsonProperty(value = "1")
  private String url;

  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.VIDEO;
  }

  @Accessors(fluent = true)
  public enum VideoMessageAttribution {
    NONE(0),
    GIPHY(1),
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
