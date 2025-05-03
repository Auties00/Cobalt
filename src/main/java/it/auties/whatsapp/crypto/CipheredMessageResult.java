package it.auties.whatsapp.crypto;

import java.util.Arrays;
import java.util.Objects;

public final class CipheredMessageResult {
    private final byte[] message;
    private final String type;

    CipheredMessageResult(byte[] message, String type) {
        this.message = message;
        this.type = type;
    }

    public byte[] message() {
        return message;
    }

    public String type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CipheredMessageResult that
                && Objects.deepEquals(message, that.message)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(message), type);
    }

    @Override
    public String toString() {
        return "CipheredMessageResult[" +
                "message=" + Arrays.toString(message) + ", " +
                "type=" + type + ']';
    }
}
