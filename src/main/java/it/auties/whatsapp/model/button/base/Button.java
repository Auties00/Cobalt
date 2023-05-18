package it.auties.whatsapp.model.button.base;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.NativeFlowInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

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
public final class Button implements ProtobufMessage {
    /**
     * The id of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The text content of this button. This property is defined only if {@link Button#bodyType()} ==
     * {@link ButtonBodyType#TEXT}.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ButtonText.class)
    private ButtonText bodyText;

    /**
     * The native flow content of this button. This property is defined only if {@link Button#bodyType()}
     * == {@link ButtonBodyType#NATIVE_FLOW}.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = NativeFlowInfo.class)
    private NativeFlowInfo bodyNativeFlow;

    /**
     * The type of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ButtonBodyType.class)
    private ButtonBodyType bodyType;

    /**
     * Constructs a new button
     *
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(@NonNull ButtonBody body) {
        return Button.of(Bytes.ofRandom(6).toHex(), body);
    }

    /**
     * Constructs a new button
     *
     * @param id   the non-null id of the button
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(@NonNull String id, ButtonBody body) {
        var builder = Button.builder().id(id);
        if (body instanceof ButtonText buttonText) {
            builder.bodyText(buttonText).bodyType(ButtonBodyType.TEXT);
        } else if (body instanceof NativeFlowInfo flowInfo) {
            builder.bodyNativeFlow(flowInfo).bodyType(ButtonBodyType.NATIVE_FLOW);
        }

        return builder.build();
    }
}