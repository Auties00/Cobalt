package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.NativeFlowInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Button implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String buttonId;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ButtonText.class)
    private ButtonText buttonText;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ButtonType.class)
    private ButtonType type;

    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = NativeFlowInfo.class)
    private NativeFlowInfo nativeFlowInfo;

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ButtonType implements ProtobufMessage {
        UNKNOWN(0),
        RESPONSE(1),
        NATIVE_FLOW(2);

        @Getter
        private final int index;

        @JsonCreator
        public static ButtonType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
