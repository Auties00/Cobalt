package it.auties.whatsapp.binary;

import it.auties.whatsapp.utils.Validate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * A class used to encode a WhatsappNode and then send it to WhatsappWeb's WebSocket.
 *
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
public final class BinaryBuffer {
    private ByteBuffer buffer;

    public BinaryBuffer(int size) {
        this(ByteBuffer.allocate(size));
    }

    public BinaryBuffer() {
        this(256);
    }

    public static BinaryBuffer of(String string) {
        return of(string.getBytes(StandardCharsets.UTF_8));
    }

    public static BinaryBuffer of(byte... bytes) {
        return new BinaryBuffer(ByteBuffer.wrap(bytes));
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

    public BinaryBuffer remaining() {
        var remaining = of(readBytes(buffer.remaining()));
        remaining.buffer().position(0);
        return remaining;
    }

    public byte[] readAllBytes() {
        return buffer.array();
    }

    public byte[] readWrittenBytes() {
        return readBytes(0, buffer.position(), false);
    }

    public BinaryArray readWrittenBytesToArray() {
        return BinaryArray.of(readWrittenBytes());
    }

    public byte[] readBytes(long size) {
        var parsed = Validate.isValid(size, size >= 0 && size < Integer.MAX_VALUE, "Cannot read %s bytes", size);
        return readBytes(buffer.position(), buffer.position() + parsed.intValue(), true);
    }

    public byte[] readBytes(int start, int end, boolean shift) {
        Validate.isTrue(start >= 0, "Expected unsigned int for start, got: %s", start);
        Validate.isTrue(end >= 0, "Expected unsigned int for end, got: %s", end);
        Validate.isTrue(end >= start, "Expected end to be bigger than start, got: %s - %s", start, end);
        var bytes = new byte[end - start];
        buffer.get(start, bytes, 0, bytes.length);
        if (shift) {
            buffer.position(buffer.position() + bytes.length);
        }
        return bytes;
    }

    public String readString(long size) {
        return new String(readBytes(size), StandardCharsets.UTF_8);
    }

    public BinaryBuffer writeInt8(byte in) {
        return write(temp -> temp.put(in), 1);
    }

    public BinaryBuffer writeUInt8(int in) {
        return writeInt8(checkUnsigned(in).byteValue());
    }

    public BinaryBuffer writeUInt8(BinaryTag in) {
        return writeUInt8(in.data());
    }

    public BinaryBuffer writeUInt16(int in) {
        return write(temp -> temp.putShort(checkUnsigned(in).shortValue()), 2);
    }

    public BinaryBuffer writeInt32(int in) {
        return write(temp -> temp.putInt(in), 4);
    }

    public BinaryBuffer writeUInt32(long in) {
        return writeInt32(checkUnsigned(in).intValue());
    }

    public BinaryBuffer writeInt64(long in) {
        return write(temp -> temp.putLong(in), 8);
    }

    public BinaryBuffer writeUInt64(BigInteger in) {
        return write(temp -> temp.put(in.toByteArray()), 8);
    }

    public BinaryBuffer writeFloat32(float in) {
        return write(temp -> temp.putFloat(in), 4);
    }

    public BinaryBuffer writeFloat64(double in) {
        return write(temp -> temp.putDouble(in), 8);
    }

    private BinaryBuffer write(Consumer<ByteBuffer> consumer, int size) {
        var temp = ByteBuffer.allocate(size);
        if (buffer.position() + size + 1 >= buffer.limit()) {
            reserve(buffer.limit() * 2);
        }

        consumer.accept(temp);
        buffer.put(temp.rewind());
        return this;
    }

    public BinaryBuffer writeBytes(byte... in) {
        for (var entry : in) writeInt8(entry);
        return this;
    }

    public BinaryBuffer writeString(String in) {
        return writeBytes(in.getBytes(StandardCharsets.UTF_8));
    }

    private BinaryBuffer reserve(int size) {
        var resized = ByteBuffer.allocate(Math.max(size, 128));
        for(var entry : readWrittenBytes()) resized.put(entry);
        this.buffer = resized;
        return this;
    }

    private <N extends Number> N checkUnsigned(N number) {
        return Validate.isValid(number, (Double.doubleToLongBits(number.doubleValue()) & Long.MIN_VALUE) != Long.MIN_VALUE,
                "Expected unsigned number, got %s", number);
    }
}