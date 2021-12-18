package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HydratedTemplateButton {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("HydratedCallButton")
  private HydratedCallButton callButton;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("HydratedURLButton")
  private HydratedURLButton urlButton;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("HydratedQuickReplyButton")
  private HydratedQuickReplyButton quickReplyButton;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint32")
  private int index;

  public HydratedButton hydratedButtonCase() {
    if (quickReplyButton != null) return HydratedButton.QUICK_REPLY_BUTTON;
    if (urlButton != null) return HydratedButton.URL_BUTTON;
    if (callButton != null) return HydratedButton.CALL_BUTTON;
    return HydratedButton.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum HydratedButton {
    UNKNOWN(0),
    QUICK_REPLY_BUTTON(1),
    URL_BUTTON(2),
    CALL_BUTTON(3);

    private final @Getter int index;

    HydratedButton(int index) {
      this.index = index;
    }

    @JsonCreator
    public static HydratedButton forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(HydratedButton.UNKNOWN);
    }
  }
}
