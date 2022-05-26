package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class NativeFlowMessage {

  @ProtobufProperty(
      index = 1,
      type = MESSAGE,
      concreteType = NativeFlowButton.class,
      repeated = true)
  private List<NativeFlowButton> buttons;

  @ProtobufProperty(index = 2, type = STRING)
  private String messageParamsJson;

  @ProtobufProperty(index = 3, type = INT32)
  private Integer messageVersion;

  public static class NativeFlowMessageBuilder {

    public NativeFlowMessageBuilder buttons(List<NativeFlowButton> buttons) {
      if (this.buttons == null) this.buttons = new ArrayList<>();
      this.buttons.addAll(buttons);
      return this;
    }
  }
}
