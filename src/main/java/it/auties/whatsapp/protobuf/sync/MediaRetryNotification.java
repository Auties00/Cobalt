package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that holds the information related to a retry for uploading a media.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MediaRetryNotification {
  /**
   * The jid of the chat where the retry should happen
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String jid;

  /**
   * The direct path to the media whose upload failed
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The type problem that occurred while uploading the media
   */
  @JsonProperty("3")
  @JsonPropertyDescription("MediaRetryNotificationResultType")
  private Problem problem;

  @Accessors(fluent = true)
  public enum Problem {
    GENERAL_ERROR(0),
    SUCCESS(1),
    NOT_FOUND(2),
    DECRYPTION_ERROR(3);

    private final @Getter int index;

    Problem(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Problem forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
