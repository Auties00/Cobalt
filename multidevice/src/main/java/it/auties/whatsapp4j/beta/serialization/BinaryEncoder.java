package it.auties.whatsapp4j.beta.serialization;

import lombok.NonNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A class used to encode a WhatsappNode and then send it to WhatsappWeb's WebSocket.
 *
 * @param buffer the message to encode
 */
public record BinaryEncoder(@NonNull ByteBuffer buffer) {
    public BinaryEncoder(){
        this(ByteBuffer.allocate(5192));
    }

    public static BinaryEncoder withSize(int size){
        return new BinaryEncoder(ByteBuffer.allocate(size));
    }

    public static BinaryEncoder fromString(String string){
        return fromBytes(string.getBytes(StandardCharsets.UTF_8));
    }

    public static BinaryEncoder fromBytes(byte... bytes){
        return new BinaryEncoder(ByteBuffer.wrap(bytes));
    }

    public static BinaryEncoder fromBytes(byte[]... bytes){
        var result = withSize(length(bytes));
        Arrays.stream(bytes).forEach(result::writeBytes);
        return result;
    }

    public byte readInt8() {
        return buffer.order(ByteOrder.LITTLE_ENDIAN)
                .get();
    }

    public int readUInt8() {
        return Byte.toUnsignedInt(readInt8());
    }

    public int readUInt16(boolean littleEndian) {
        return Short.toUnsignedInt(ordered(littleEndian).getShort());
    }

    public int readInt32(boolean littleEndian) {
        return ordered(littleEndian)
                .getInt();
    }

    public long readUInt32(boolean littleEndian) {
        return Integer.toUnsignedLong(readInt32(littleEndian));
    }

    public long readInt64(boolean littleEndian) {
        return ordered(littleEndian)
                .getLong();
    }

    public BigInteger readUInt64(boolean littleEndian) {
        return new BigInteger(Long.toUnsignedString(readInt64(littleEndian)));
    }

    public float readFloat32(boolean littleEndian) {
        return ordered(littleEndian)
                .getFloat();
    }

    public double readFloat64(boolean littleEndian) {
        return ordered(littleEndian)
                .getDouble();
    }

    public int readVarInt(){
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

    public ByteBuffer readBuffer(int size){
        return buffer.slice(buffer.position(), buffer.position() + size);
    }

    public byte[] readBytes(){
        return readBytes(buffer.remaining());
    }

    public byte[] readBytes(int size){
        var bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    public String readString(int size){
        return new String(readBytes(size));
    }

    public BinaryEncoder writeInt8(byte in) {
        var temp = ordered(1, false).put(in);
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeUInt8(int in) {
        return writeInt8(checkUnsigned(in).byteValue());
    }

    public BinaryEncoder writeUInt16(int in) {
        var temp = ordered(2, false).putShort(checkUnsigned(in).shortValue());
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeInt32(int in, boolean littleEndian) {
        var temp = ordered(4, littleEndian).putInt(in);
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeUInt32(long in, boolean littleEndian) {
        return writeInt32(checkUnsigned(in).intValue(), littleEndian);
    }

    public BinaryEncoder writeInt64(long in, boolean littleEndian) {
        var temp = ordered(8, littleEndian).putLong(in);
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeUInt64(BigInteger in, boolean littleEndian) {
        var temp = ordered(8, littleEndian).put(in.toByteArray());
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeFloat32(float in, boolean littleEndian) {
        var temp = ordered(4, littleEndian).putFloat(in);
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeFloat64(double in, boolean littleEndian) {
        var temp = ordered(8, littleEndian).putDouble(in);
        buffer.put(temp);
        return this;
    }

    public BinaryEncoder writeVarInt(int in){
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

    public BinaryEncoder writeBuffer(ByteBuffer in){
        buffer.put(in);
        return this;
    }

    public BinaryEncoder writeBytes(byte... in){
        buffer.put(in);
        return this;
    }

    public BinaryEncoder writeString(String in){
        return writeBytes(in.getBytes(StandardCharsets.UTF_8));
    }

    private static int length(byte[][] bytes) {
        return Arrays.stream(bytes).mapToInt(BinaryEncoder::length).sum();
    }

    private static int length(byte[] array) {
        return array.length;
    }

    private <N extends Number> N checkUnsigned(N number){
        if ((Double.doubleToLongBits(number.doubleValue()) & Long.MIN_VALUE) != Long.MIN_VALUE) {
            return number;
        }

        throw new IllegalArgumentException("Expected unsigned number, got %s".formatted(number));
    }

    private ByteBuffer ordered(boolean littleEndian) {
        return buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    private ByteBuffer ordered(int size, boolean littleEndian) {
        return ByteBuffer.allocate(size)
                .order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }
}