package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that holds the information related to an ad reply.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newAdReplyInfo", buildMethodName = "create")
@Accessors(fluent = true)
public class AdReplyInfo {
  /**
   * The caption of the original ad
   */
  @JsonProperty(value = "17")
  private String caption;

  /**
   * The thumbnail of the original ad encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  /**
   * The type of original ad
   */
  @JsonProperty(value = "2")
  private AdReplyInfoMediaType mediaType;

  /**
   * The name of the advertiser that served the original ad
   */
  @JsonProperty(value = "1")
  private String advertiserName;

  /**
   * The constants of this enumerated type describe the various types of ad that a {@link AdReplyInfo} can link to
   */
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

    AdReplyInfoMediaType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static AdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
