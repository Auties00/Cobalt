package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class TemplateButton {
  @JsonProperty(value = "3")
  private CallButton callButton;

  @JsonProperty(value = "2")
  private URLButton urlButton;

  @JsonProperty(value = "1")
  private QuickReplyButton quickReplyButton;

  @JsonProperty(value = "4")
  private int index;

  public Button buttonCase() {
    if (quickReplyButton != null) return Button.QUICK_REPLY_BUTTON;
    if (urlButton != null) return Button.URL_BUTTON;
    if (callButton != null) return Button.CALL_BUTTON;
    return Button.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Button {
    UNKNOWN(0),
    QUICK_REPLY_BUTTON(1),
    URL_BUTTON(2),
    CALL_BUTTON(3);

    private final @Getter int index;

    Button(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Button forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Button.UNKNOWN);
    }
  }
}
