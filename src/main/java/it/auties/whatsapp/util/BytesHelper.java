package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageContainer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static it.auties.whatsapp.util.Spec.Signal.CURRENT_VERSION;

@UtilityClass
public class BytesHelper {
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
        var padding = Bytes.newBuffer(padRandomByte).fill((byte) padRandomByte).toByteArray();
        return Bytes.of(Protobuf.writeMessage(container)).append(padding).toByteArray();
    }

    @SneakyThrows
    public MessageContainer bytesToMessage(byte[] bytes) {
        var message = Bytes.of(bytes).cut(-bytes[bytes.length - 1]).toByteArray();
        return Protobuf.readMessage(message, MessageContainer.class);
    }

    public byte[] longToBytes(long number) {
        return Bytes.newBuffer().appendUnsignedInt((int) number).toByteArray();
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
}
