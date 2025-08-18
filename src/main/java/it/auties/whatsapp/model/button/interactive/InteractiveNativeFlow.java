package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;

import java.util.List;
import java.util.Objects;


/**
 * A model class that represents a native flow
 * <a href="https://docs.360dialog.com/partner/messaging/interactive-messages/beta-receive-whatsapp-payments-via-stripe">Here</a>> is an explanation on how to use this kind of message
 */
@ProtobufMessage(name = "Message.InteractiveMessage.NativeFlowMessage")
public final class InteractiveNativeFlow implements InteractiveMessageContent {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<InteractiveButton> buttons;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String parameters;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int version;

    InteractiveNativeFlow(List<InteractiveButton> buttons, String parameters, int version) {
        this.buttons = Objects.requireNonNullElse(buttons, List.of());
        this.parameters = parameters;
        this.version = version;
    }

    public List<InteractiveButton> buttons() {
        return buttons;
    }

    public String parameters() {
        return parameters;
    }

    public int version() {
        return version;
    }

    @Override
    public Type contentType() {
        return Type.NATIVE_FLOW;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveNativeFlow that
                && Objects.equals(buttons, that.buttons)
                && Objects.equals(parameters, that.parameters)
                && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buttons, parameters, version);
    }

    @Override
    public String toString() {
        return "InteractiveNativeFlow[" +
                "buttons=" + buttons + ", " +
                "parameters=" + parameters + ", " +
                "version=" + version + ']';
    }
}