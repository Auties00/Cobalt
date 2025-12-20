package com.github.auties00.cobalt.model.message.server;

import com.github.auties00.cobalt.model.message.model.ServerMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

/**
 * A model class that represents a message that refers to a message sent by the device paired with
 * the active WhatsappWeb session to sync.
 */
@ProtobufMessage(name = "Message.DeviceSyncMessage")
public final class DeviceSyncMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] serializedXmlBytes;

    DeviceSyncMessage(byte[] serializedXmlBytes) {
        this.serializedXmlBytes = Objects.requireNonNull(serializedXmlBytes, "serializedXmlBytes cannot be null");
    }

    public byte[] serializedXmlBytes() {
        return serializedXmlBytes;
    }

    @Override
    public Type type() {
        return Type.DEVICE_SYNC;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeviceSyncMessage that
                && Arrays.equals(serializedXmlBytes, that.serializedXmlBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(serializedXmlBytes);
    }

    @Override
    public String toString() {
        return "DeviceSyncMessage[" +
                "serializedXmlBytes=" + Arrays.toString(serializedXmlBytes) + ']';
    }
}