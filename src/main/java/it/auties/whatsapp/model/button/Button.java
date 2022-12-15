package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.NativeFlowInfo;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Button implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ButtonText.class)
    private ButtonText text;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ButtonType.class)
    private ButtonType type;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = NativeFlowInfo.class)
    private NativeFlowInfo nativeFlowInfo;

    /**
     * Constructs a new builder to create a response text button.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}.
     *
     * @param text the non-null text of this button
     * @return a non-null button
     */
    public static Button newResponseButton(@NonNull String text) {
        return newResponseButton(Bytes.ofRandom(6)
                .toHex(), text);
    }

    /**
     * Constructs a new builder to create a response text button.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}.
     *
     * @param id   the non-null id of the button
     * @param text the non-null text of this button
     * @return a non-null button
     */
    public static Button newResponseButton(@NonNull String id, @NonNull String text) {
        return Button.builder()
                .id(id)
                .text(ButtonText.of(text))
                .type(ButtonType.RESPONSE)
                .build();
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ButtonType implements ProtobufMessage {
        UNKNOWN(0),
        RESPONSE(1),
        NATIVE_FLOW(2);

        @Getter
        private final int index;

        @JsonCreator
        public static ButtonType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
