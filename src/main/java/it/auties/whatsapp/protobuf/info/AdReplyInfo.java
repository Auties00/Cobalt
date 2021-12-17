package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that holds the information related to an companion reply.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newAdReplyInfo", buildMethodName = "create")
@Accessors(fluent = true)
public class AdReplyInfo {
  /**
   * The caption of the original companion
   */
  @JsonProperty(value = "17")
  @JsonPropertyDescription("string")
  private String caption;

  /**
   * The thumbnail of the original companion encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  /**
   * The type of original companion
   */
  @JsonProperty(value = "2")
  @JsonPropertyDescription("AdReplyInfoMediaType")
  private AdReplyInfoMediaType mediaType;

  /**
   * The name of the advertiser that served the original companion
   */
  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String advertiserName;

  /**
   * The constants of this enumerated type describe the various types of companion that a {@link AdReplyInfo} can link to
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum AdReplyInfoMediaType {
    /**
     * Unknown type
     */
    NONE(0),

    /**
     * Image type
     */
    IMAGE(1),

    /**
     * Video type
     */
    VIDEO(2);

    private final @Getter int index;

    @JsonCreator
    public static AdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
