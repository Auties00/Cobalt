package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HydratedTemplateButton {
  @JsonProperty(value = "3")
  private HydratedCallButton callButton;

  @JsonProperty(value = "2")
  private HydratedURLButton urlButton;

  @JsonProperty(value = "1")
  private HydratedQuickReplyButton quickReplyButton;

  @JsonProperty(value = "4")
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
