package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonBody;

import java.util.Objects;

/**
 * A model class that holds the information related to a native flow.
 */
@ProtobufMessage(name = "Message.ButtonsMessage.Button.NativeFlowInfo")
public final class NativeFlowInfo implements Info, ButtonBody {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String parameters;

    NativeFlowInfo(String name, String parameters) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters cannot be null");
    }

    public String name() {
        return name;
    }

    public String parameters() {
        return parameters;
    }

    @Override
    public Type bodyType() {
        return Type.NATIVE_FLOW;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NativeFlowInfo that
                && Objects.equals(name, that.name)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return "NativeFlowInfo[" +
                "name=" + name +
                ", parameters=" + parameters +
                ']';
    }
}