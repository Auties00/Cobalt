package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ButtonTemplate implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = QuickReplyButton.class)
    private QuickReplyButton quickReplyButton;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = URLButton.class)
    private URLButton urlButton;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = CallButton.class)
    private CallButton callButton;

    @ProtobufProperty(index = 4, type = UINT32)
    private int index;

    public ButtonType buttonType() {
        if (quickReplyButton != null)
            return ButtonType.QUICK_REPLY_BUTTON;
        if (urlButton != null)
            return ButtonType.URL_BUTTON;
        if (callButton != null)
            return ButtonType.CALL_BUTTON;
        return ButtonType.UNKNOWN;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ButtonType implements ProtobufMessage {
        UNKNOWN(0),
        QUICK_REPLY_BUTTON(1),
        URL_BUTTON(2),
        CALL_BUTTON(3);

        @Getter
        private final int index;

        @JsonCreator
        public static ButtonType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(ButtonType.UNKNOWN);
        }
    }
}
