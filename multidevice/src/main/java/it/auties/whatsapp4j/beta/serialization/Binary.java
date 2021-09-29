package it.auties.whatsapp4j.beta.serialization;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * A class used to encode a WhatsappNode and then send it to WhatsappWeb's WebSocket.
 *
 */
public record Binary(ByteBuffer buffer) {
    public Binary() {
        this(ByteBuffer.allocate(128));
    }

    public static Binary withSize(int size) {
        return new Binary(ByteBuffer.allocate(size));
    }

    public static Binary fromString(String string) {
        return fromBytes(string.getBytes(StandardCharsets.UTF_8));
    }

    public static Binary fromBytes(byte... bytes) {
        return new Binary(ByteBuffer.wrap(bytes));
    }

    public static Binary fromBytes(byte[]... bytes) {
        var result = withSize(length(bytes));
        Arrays.stream(bytes).forEach(result::writeBytes);
        return result;
    }

    public byte readInt8() {
        return buffer.get();
    }

    public int readUInt8() {
        return Byte.toUnsignedInt(readInt8());
    }

    public int readUInt16() {
        return Short.toUnsignedInt(buffer.getShort());
    }

    public int readInt32() {
        return buffer.getInt();
    }

    public long readUInt32() {
        return Integer.toUnsignedLong(readInt32());
    }

    public long readInt64() {
        return buffer.getLong();
    }

    public BigInteger readUInt64() {
        return new BigInteger(Long.toUnsignedString(readInt64()));
    }

    public float readFloat32() {
        return buffer.getFloat();
    }

    public double readFloat64() {
        return buffer.getDouble();
    }

    public int readVarInt() {
        var tmp = 0;
        if ((tmp = readInt8()) >= 0) {
            return tmp;
        }

        var result = tmp & 0x7f;
        if ((tmp = readInt8()) >= 0) {
            return result | (tmp << 7);
        }

        result |= (tmp & 0x7f) << 7;
        if ((tmp = readInt8()) >= 0) {
            return result | (tmp << 14);
        }

        result |= (tmp & 0x7f) << 14;
        if ((tmp = readInt8()) >= 0) {
            return result | (tmp << 21);
        }

        result |= (tmp & 0x7f) << 21;
        result |= (tmp = readInt8()) << 28;
        while (tmp < 0) tmp = readInt8();
        return result;
    }

    public byte[] readAllBytes() {
        return readBytes(-1);
    }

    public byte[] readBytes(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new RuntimeException("long => int");
        }

        buffer.rewind();
        if(size < 0){
            size = buffer.remaining();
        }

        var bytes = new byte[(int) size];
        buffer.get(bytes);
        return bytes;
    }

    public String readString(long size) {
        return new String(readBytes(size));
    }

    public Binary writeInt8(byte in) {
        System.out.printf("Writing int8 %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        buffer.put(temp(1).put(in).rewind());
        System.out.printf("Wrote %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        return this;
    }

    public Binary writeUInt8(int in) {
        System.out.printf("Writing uint8 %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        return writeInt8(checkUnsigned(in).byteValue());
    }

    public Binary writeUInt16(int in) {
        System.out.printf("Writing uint16 %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        buffer.put(temp(2).putShort(checkUnsigned(in).shortValue()).rewind());
        System.out.printf("Wrote %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        return this;
    }

    public Binary writeInt32(int in) {
        System.out.printf("Writing int32 %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        buffer.put(temp(4).putInt(in).rewind());
        System.out.printf("Wrote %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        return this;
    }

    public Binary writeUInt32(long in) {
        System.out.printf("Writing uint32 %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        buffer.put(temp(4).putInt(checkUnsigned(in).intValue()).rewind());
        System.out.printf("Wrote %s to buffer %s%n", in, Arrays.toString(buffer.array()));
        return this;
    }

    public Binary writeInt64(long in) {
        buffer.put(temp(8).putLong(in).rewind());
        return this;
    }

    public Binary writeUInt64(BigInteger in) {
        buffer.put(temp(8).put(in.toByteArray()).rewind());
        return this;
    }

    public Binary writeFloat32(float in) {
        buffer.put(temp(4).putFloat(in).rewind());
        return this;
    }

    public Binary writeFloat64(double in) {
        buffer.put(temp(8).putDouble(in).rewind());
        return this;
    }

    public Binary writeVarInt(int in) {
        while (true) {
            var bits = in & 0x7f;
            in >>>= 7;
            if (in == 0) {
                buffer.put((byte) bits);
                return this;
            }

            buffer.put((byte) (bits | 0x80));
        }
    }

    public Binary writeBytes(byte... in) {
        IntStream.range(0, in.length).mapToObj(index -> Byte.toUnsignedInt(in[index])).forEach(buffer::putInt);
        return this;
    }

    public Binary writeString(String in) {
        return writeBytes(in.getBytes(StandardCharsets.UTF_8));
    }

    private static int length(byte[][] bytes) {
        return Arrays.stream(bytes).mapToInt(Binary::length).sum();
    }

    private static int length(byte[] array) {
        return array.length;
    }

    private <N extends Number> N checkUnsigned(N number) {
        if ((Double.doubleToLongBits(number.doubleValue()) & Long.MIN_VALUE) != Long.MIN_VALUE) {
            return number;
        }

        throw new IllegalArgumentException("Expected unsigned number, got %s".formatted(number));
    }

    private ByteBuffer temp(int size) {
        return ByteBuffer.allocate(size);
    }
}