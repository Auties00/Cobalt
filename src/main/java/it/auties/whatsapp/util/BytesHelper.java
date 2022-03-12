package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@UtilityClass
public class BytesHelper {
    public byte serialize(int version){
        return (byte) (version << 4 | SignalSpecification.CURRENT_VERSION);
    }

    public int deserialize(byte version){
        return Byte.toUnsignedInt(version) >> 4;
    }

    public byte[] deflate(byte[] compressed) {
       try {
           var decompressor = new Inflater();
           decompressor.setInput(compressed);
           var result = new ByteArrayOutputStream();
           var buffer = new byte[1024];
           while (!decompressor.finished()) {
               var count = decompressor.inflate(buffer);
               result.write(buffer, 0, count);
           }

           return result.toByteArray();
       }catch (DataFormatException exception){
           throw new IllegalArgumentException("Cannot deflate", exception);
       }
    }

    public byte[] pad(byte[] bytes){
        var padRandomByte = Keys.header();
        var padding = Bytes.newBuffer(padRandomByte)
                .fill((byte) padRandomByte)
                .toByteArray();
       return Bytes.of(bytes)
               .append(padding)
               .toByteArray();
    }

    public int fromBytes(byte[] bytes, int length){
        var result = 0;
        for (var i = 0; i < length; i++) {
            result = 256 * result + Byte.toUnsignedInt(bytes[i]);
        }

        return result;
    }

    public byte[] toBytes(long number){
        return Bytes.newBuffer()
                .appendLong(number)
                .assertSize(Long.BYTES)
                .toByteArray();
    }

    public byte[] toBytes(int input, int length) {
        var result = new byte[length];
        for(var i = length - 1; i >= 0; i--){
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }

        return result;
    }
}
