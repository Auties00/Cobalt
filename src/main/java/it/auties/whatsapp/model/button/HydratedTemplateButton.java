package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HydratedTemplateButton implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = HydratedQuickReplyButton.class)
  private HydratedQuickReplyButton quickReplyButton;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = HydratedURLButton.class)
  private HydratedURLButton urlButton;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = HydratedCallButton.class)
  private HydratedCallButton callButton;

  @ProtobufProperty(index = 4, type = UINT32)
  private int index;

  public HydratedButtonType hydratedButtonType() {
    if (quickReplyButton != null) return HydratedButtonType.QUICK_REPLY_BUTTON;
    if (urlButton != null) return HydratedButtonType.URL_BUTTON;
    if (callButton != null) return HydratedButtonType.CALL_BUTTON;
    return HydratedButtonType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HydratedButtonType {
    UNKNOWN(0),
    QUICK_REPLY_BUTTON(1),
    URL_BUTTON(2),
    CALL_BUTTON(3);

    @Getter
    private final int index;

    public static HydratedButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(HydratedButtonType.UNKNOWN);
    }
  }
}
