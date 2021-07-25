package it.auties.whatsapp4j.protobuf.model.button;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.info.NativeFlowInfo;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Button {
  @JsonProperty(value = "4")
  private NativeFlowInfo nativeFlowInfo;

  @JsonProperty(value = "3")
  private ButtonType type;

  @JsonProperty(value = "2")
  private ButtonText buttonText;

  @JsonProperty(value = "1")
  private String buttonId;

  @Accessors(fluent = true)
  public enum ButtonType {
    UNKNOWN(0),
    RESPONSE(1),
    NATIVE_FLOW(2);

    private final @Getter int index;

    ButtonType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
