package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ButtonMessage;

import java.util.Objects;

@ProtobufMessage(name = "Message.InteractiveResponseMessage.NativeFlowResponseMessage")
public final class NativeFlowResponseMessage implements ButtonMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String paramsJson;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int version;

    NativeFlowResponseMessage(String name, String paramsJson, int version) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.paramsJson = Objects.requireNonNull(paramsJson, "paramsJson cannot be null");
        this.version = version;
    }

    public String name() {
        return name;
    }

    public String paramsJson() {
        return paramsJson;
    }

    public int version() {
        return version;
    }

    @Override
    public Type type() {
        return Type.NATIVE_FLOW_RESPONSE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NativeFlowResponseMessage that
                && Objects.equals(name, that.name)
                && Objects.equals(paramsJson, that.paramsJson)
                && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, paramsJson, version);
    }

    @Override
    public String toString() {
        return "NativeFlowResponseMessage[" +
                "name=" + name + ", " +
                "paramsJson=" + paramsJson + ", " +
                "version=" + version + ']';
    }
}