package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.Message;

import java.util.Arrays;
import java.util.Objects;

/**
 * A message that contains information related to a call
 */
@ProtobufMessage(name = "Message.Call")
public final class CallMessage implements Message {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] key;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String source;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] data;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    final int delay;

    CallMessage(byte[] key, String source, byte[] data, int delay) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.data = Objects.requireNonNull(data, "data cannot be null");
        this.delay = delay;
    }

    public byte[] key() {
        return key;
    }

    public String source() {
        return source;
    }

    public byte[] data() {
        return data;
    }

    public int delay() {
        return delay;
    }

    @Override
    public Type type() {
        return Type.CALL;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CallMessage that
                && Arrays.equals(key, that.key)
                && Objects.equals(source, that.source)
                && Arrays.equals(data, that.data)
                && delay == that.delay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(key), source, Arrays.hashCode(data), delay);
    }

    @Override
    public String toString() {
        return "CallMessage[" +
                "key=" + Arrays.toString(key) +
                ", source=" + source +
                ", data=" + Arrays.toString(data) +
                ", delay=" + delay +
                ']';
    }
}