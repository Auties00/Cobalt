package it.auties.whatsapp.model.signal.key;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

// TODO: Refactor this class when next ModernProtobuf update is ready
public final class SignalPublicKey implements SignalKey, Comparable<SignalPublicKey> {
    private static final byte KEY_TYPE = 5;
    private static final int KEY_LENGTH = 32;
    private static final int KEY_WITH_TYPE_LENGTH = 33;
    private final byte[] point;

    public static byte type() {
        return KEY_TYPE;
    }

    public static int length() {
        return KEY_LENGTH;
    }

    private SignalPublicKey(byte[] point, int offset, int length) {
        Objects.requireNonNull(point, "key cannot be null");
        this.point = switch (length) {
            case KEY_LENGTH -> offset == 0 ? point : Arrays.copyOfRange(point, offset, offset + length);
            case KEY_WITH_TYPE_LENGTH -> {
                if (point[offset] != KEY_TYPE) {
                    throw new IllegalArgumentException("Invalid key type");
                }

                yield Arrays.copyOfRange(point, offset + 1, KEY_WITH_TYPE_LENGTH);
            }
            default -> throw new IllegalArgumentException("Invalid key length: " + point.length);
        };
    }

    public static SignalPublicKey of(SignalPrivateKey privateKey) {
        var publicKey = Curve25519.getPublicKey(privateKey.encodedPoint());
        return of(publicKey);
    }

    @ProtobufDeserializer
    public static SignalPublicKey of(byte[] encodedPoint) {
        return new SignalPublicKey(encodedPoint, 0, encodedPoint.length);
    }

    public static SignalPublicKey of(byte[] encodedPoint, int offset, int length) {
        return new SignalPublicKey(encodedPoint, offset, length);
    }

    @ProtobufSerializer
    public byte[] serialized() {
        var result = new byte[KEY_WITH_TYPE_LENGTH];
        result[0] = KEY_TYPE;
        System.arraycopy(point, 0, result, 1, KEY_LENGTH);
        return result;
    }

    public int serialize(byte[] destination, int offset) {
        destination[offset++] = KEY_TYPE;
        System.arraycopy(point, 0, destination, offset, KEY_LENGTH);
        return offset + KEY_LENGTH;
    }

    @Override
    public byte[] encodedPoint() {
        return point;
    }

    @Override
    public int writePoint(byte[] destination, int offset) {
        System.arraycopy(point, 0, destination, offset, KEY_LENGTH);
        return offset + KEY_LENGTH;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SignalPublicKey that
                && Arrays.equals(point, that.point);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(point);
    }

    @Override
    public String toString() {
        return "SignalPublicKey[" +
                "point=" + Arrays.toString(point) + ']';
    }

    // TODO: This is how libsignal does it but it doesn't make much sense to me
    @Override
    public int compareTo(SignalPublicKey o) {
        return new BigInteger(point)
                .compareTo(new BigInteger(o.point));
    }
}
