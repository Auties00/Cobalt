package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.util.BytesDeserializer;
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
public final class AdReplyInfo implements WhatsappInfo {
  /**
   * The name of the advertiser that served the original companion
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String advertiserName;

  /**
   * The type of original companion
   */
  @JsonProperty("2")
  @JsonPropertyDescription("AdReplyInfoMediaType")
  private AdReplyInfoMediaType mediaType;

  /**
   * The thumbnail of the original companion encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] thumbnail;

  /**
   * The caption of the original companion
   */
  @JsonProperty("17")
  @JsonPropertyDescription("string")
  private String caption;

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

    @Getter
    private final int index;

    @JsonCreator
    public static AdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
