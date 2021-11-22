package it.auties.whatsapp.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * A utility class used to work with {@link ByteBuf}
 */
@UtilityClass
public class Buffers {
    /**
     * Constructs a new empty buffer
     *
     * @return a non-null byte buffer
     */
    public ByteBuf newBuffer(){
        return ByteBufUtil.threadLocalDirectBuffer();
    }

    /**
     * Reads the readable bytes of the provided buffer
     *
     * @param buffer the non-null byte buffer to read
     * @return a non-null binary array
     */
    public BinaryArray readBinary(@NonNull ByteBuf buffer){
        return BinaryArray.of(readBytes(buffer));
    }

    /**
     * Reads the readable bytes of the provided buffer
     *
     * @param buffer the non-null byte buffer to read
     * @return a non-null byte array
     */
    public byte[] readBytes(@NonNull ByteBuf buffer){
        return readBytes(buffer, buffer.readableBytes());
    }

    /**
     * Reads a specified amount of bytes from the provided buffer
     *
     * @param buffer the non-null byte buffer to read
     * @param length the number of bytes to read
     * @return a non-null byte array
     */
    public byte[] readBytes(@NonNull ByteBuf buffer, int length){
        var result = new byte[length];
        buffer.readBytes(result);
        return result;
    }
}
