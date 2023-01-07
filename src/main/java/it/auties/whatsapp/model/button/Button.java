package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
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
public class Button
        implements ProtobufMessage {
    /**
     * The id of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The text content of this button.
     * This property is defined only if {@link Button#type()} == {@link ButtonType#TEXT}.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ButtonText.class)
    private ButtonText text;

    /**
     * The native flow content of this button.
     * This property is defined only if {@link Button#type()} == {@link ButtonType#NATIVE_FLOW}.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = NativeFlowInfo.class)
    private NativeFlowInfo nativeFlowInfo;

    /**
     * The type of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ButtonType.class)
    private ButtonType type;

    /**
     * Constructs a new text button
     *
     * @param text the non-null text of this button
     * @return a non-null button
     */
    public static Button of(@NonNull String text) {
        return of(Bytes.ofRandom(6).toHex(), text);
    }

    /**
     * Constructs a new text button
     *
     * @param id   the non-null id of the button
     * @param text the non-null text of this button
     * @return a non-null button
     */
    public static Button of(@NonNull String id, @NonNull String text) {
        return Button.builder()
                .id(id)
                .text(ButtonText.of(text))
                .type(ButtonType.TEXT)
                .build();
    }

    /**
     * Constructs a new button with a native flow
     *
     * @param info the non-null native flow
     * @return a non-null button
     */
    public static Button of(@NonNull NativeFlowInfo info) {
        return of(Bytes.ofRandom(6).toHex(), info);
    }

    /**
     * Constructs a new button with a native flow
     *
     * @param id   the non-null id of the button
     * @param info the non-null native flow
     * @return a non-null button
     */
    public static Button of(@NonNull String id, @NonNull NativeFlowInfo info) {
        return Button.builder()
                .id(id)
                .nativeFlowInfo(info)
                .type(ButtonType.NATIVE_FLOW)
                .build();
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ButtonType
            implements ProtobufMessage {
        UNKNOWN(0),
        TEXT(1),
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
