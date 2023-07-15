package it.auties.whatsapp.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageContainer;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static it.auties.whatsapp.util.Spec.Signal.CURRENT_VERSION;

@UtilityClass
public class BytesHelper {
    private static final String CROCKFORD_CHARACTERS = "123456789ABCDEFGHJKLMNPQRSTVWXYZ";

    public byte[] random(int length){
        var bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
    
    public byte[] concat(byte[]... entries){
        return Arrays.stream(entries)
                .filter(Objects::nonNull)
                .reduce(new byte[0], BytesHelper::concat);
    }

    public byte[] concat(byte first, byte[] second) {
        if(second == null){
            return new byte[]{first};
        }

        var result = new byte[1 + second.length];
        result[0] = first;
        System.arraycopy(second, 0, result, 1, second.length);
        return result;
    }

    public byte[] concat(byte[] first, byte[] second) {
        if(first == null){
            return second;
        }

        if(second == null){
            return first;
        }

        var result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public ByteBuf newBuffer(){
        return Unpooled.buffer();
    }

    public ByteBuf newBuffer(int size){
        return Unpooled.buffer(size);
    }

    public ByteBuf newBuffer(byte @NonNull [] data){
        var buffer = newBuffer(data.length);
        buffer.writeBytes(data);
        return buffer;
    }

    public byte[] readBuffer(ByteBuf byteBuf){
        return readBuffer(byteBuf, byteBuf.readableBytes());
    }

    public byte[] readBuffer(ByteBuf byteBuf, int length){
        var result = new byte[length];
        byteBuf.readBytes(result);
        return result;
    }

    public byte versionToBytes(int version) {
        return (byte) (version << 4 | CURRENT_VERSION);
    }

    public int bytesToVersion(byte version) {
        return Byte.toUnsignedInt(version) >> 4;
    }

    public byte[] compress(byte[] uncompressed) {
        var deflater = new Deflater();
        deflater.setInput(uncompressed);
        deflater.finish();
        var result = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        while (!deflater.finished()) {
            var count = deflater.deflate(buffer);
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }

    @SneakyThrows
    public byte[] decompress(byte[] compressed) {
        var decompressor = new Inflater();
        decompressor.setInput(compressed);
        var result = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        while (!decompressor.finished()) {
            var count = decompressor.inflate(buffer);
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }

    public byte[] messageToBytes(Message message) {
        return messageToBytes(MessageContainer.of(message));
    }

    public byte[] messageToBytes(MessageContainer container) {
        if(container.isEmpty()){
            return null;
        }

        var padRandomByte = KeyHelper.header();
        var padding = new byte[padRandomByte];
        Arrays.fill(padding, (byte) padRandomByte);
        return concat(Protobuf.writeMessage(container), padding);
    }

    public MessageContainer bytesToMessage(byte[] bytes) {
        var message = Arrays.copyOfRange(bytes, 0, bytes.length - bytes[bytes.length - 1]);
        return Protobuf.readMessage(message, MessageContainer.class);
    }

    public byte[] longToBytes(long number) {
        var buffer = newBuffer();
        buffer.writeLong(number);
        return readBuffer(buffer);
    }

    public byte[] intToBytes(int input, int length) {
        var result = new byte[length];
        for (var i = length - 1; i >= 0; i--) {
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }
        return result;
    }

    public int bytesToInt(byte[] bytes, int length) {
        var result = 0;
        for (var i = 0; i < length; i++) {
            result = 256 * result + Byte.toUnsignedInt(bytes[i]);
        }
        return result;
    }

    public String bytesToCrockford(byte[] bytes) {
        var buffer = ByteBuffer.wrap(bytes);
        var value = 0;
        var bitCount = 0;
        var crockford = new StringBuilder();
        for (var i = 0; i < buffer.limit(); i++) {
            value = (value << 8) | (buffer.get(i) & 0xFF);
            bitCount += 8;
            while (bitCount >= 5) {
                crockford.append(CROCKFORD_CHARACTERS.charAt((value >>> (bitCount - 5)) & 31));
                bitCount -= 5;
            }
        }

        if (bitCount > 0) {
            crockford.append(CROCKFORD_CHARACTERS.charAt((value << (5 - bitCount)) & 31));
        }

        return crockford.toString();
    }
}
