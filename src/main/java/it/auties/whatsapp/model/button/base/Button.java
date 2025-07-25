package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonBody.Type;
import it.auties.whatsapp.model.info.NativeFlowInfo;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a button
 */
@ProtobufMessage(name = "Message.ButtonsMessage.Button")
public final class Button {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ButtonText bodyText;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final NativeFlowInfo bodyNativeFlow;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final Type bodyType;

    Button(String id, ButtonText bodyText, NativeFlowInfo bodyNativeFlow, Type bodyType) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.bodyText = bodyText;
        this.bodyNativeFlow = bodyNativeFlow;
        this.bodyType = Objects.requireNonNull(bodyType, "bodyType cannot be null");
    }

    public String id() {
        return id;
    }

    public Optional<ButtonText> bodyText() {
        return Optional.ofNullable(bodyText);
    }

    public Optional<NativeFlowInfo> bodyNativeFlow() {
        return Optional.ofNullable(bodyNativeFlow);
    }

    public Type bodyType() {
        return bodyType;
    }

    /**
     * Constructs a new button
     *
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(ButtonBody body) {
        var id = HexFormat.of().formatHex(Bytes.random(6));
        return Button.of(id, body);
    }

    /**
     * Constructs a new button
     *
     * @param id   the non-null id of the button
     * @param body the body of this button
     * @return a non-null button
     */
    public static Button of(String id, ButtonBody body) {
        var builder = new ButtonBuilder()
                .id(id);
        switch (body) {
            case ButtonText buttonText -> builder.bodyText(buttonText).bodyType(Type.TEXT);
            case NativeFlowInfo flowInfo -> builder.bodyNativeFlow(flowInfo).bodyType(Type.NATIVE_FLOW);
            case null -> builder.bodyType(Type.UNKNOWN);
        }
        return builder.build();
    }

    /**
     * Returns the body of this button
     *
     * @return an optional
     */
    public Optional<? extends ButtonBody> body() {
        return bodyText != null ? Optional.of(bodyText) : Optional.ofNullable(bodyNativeFlow);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Button that
                && Objects.equals(id, that.id)
                && Objects.equals(bodyText, that.bodyText)
                && Objects.equals(bodyNativeFlow, that.bodyNativeFlow)
                && Objects.equals(bodyType, that.bodyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bodyText, bodyNativeFlow, bodyType);
    }

    @Override
    public String toString() {
        return "Button[" +
                "id=" + id + ", " +
                "bodyText=" + bodyText + ", " +
                "bodyNativeFlow=" + bodyNativeFlow + ", " +
                "bodyType=" + bodyType + ']';
    }
}