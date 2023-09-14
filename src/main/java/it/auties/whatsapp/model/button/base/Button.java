package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.NativeFlowInfo;
import it.auties.whatsapp.util.BytesHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HexFormat;
import java.util.Optional;

/**
 * A model class that represents a button
 */

public record Button(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<ButtonText> bodyText,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<NativeFlowInfo> bodyNativeFlow,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        @NonNull
        ButtonBodyType bodyType
) implements ProtobufMessage {
    /**
     * Constructs a new button
     *
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(@NonNull ButtonBody body) {
        var id = HexFormat.of().formatHex(BytesHelper.random(6));
        return Button.of(id, body);
    }

    /**
     * Constructs a new button
     *
     * @param id   the non-null id of the button
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(@NonNull String id, ButtonBody body) {
        var builder = new ButtonBuilder()
                .id(id);
        switch (body) {
            case ButtonText buttonText -> builder.bodyText(buttonText).bodyType(ButtonBodyType.TEXT);
            case NativeFlowInfo flowInfo -> builder.bodyNativeFlow(flowInfo).bodyType(ButtonBodyType.NATIVE_FLOW);
            case null -> builder.bodyType(ButtonBodyType.UNKNOWN);
        }
        return builder.build();
    }

    /**
     * Returns the body of this button
     *
     * @return an optional
     */
    public Optional<? extends ButtonBody> body() {
        return bodyText.isPresent() ? bodyText : bodyNativeFlow;
    }

}