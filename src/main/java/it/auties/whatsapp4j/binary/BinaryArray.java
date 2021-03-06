package it.auties.whatsapp4j.binary;

import jakarta.xml.bind.DatatypeConverter;
import org.glassfish.grizzly.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * An utility class that wraps an array of bytes
 * It provides an easy interface to modify said data, convert it or generate it
 * This is intended to only be used for WhatsappWeb's WebSocket binary operations
 */
public record BinaryArray(byte[] data) {
    /**
     * Returns a {@code BinaryArray} wrapping an empty bytes array
     * @return an empty {@code BinaryArray}
     */
    public static @NotNull BinaryArray empty(){
        return forArray(new byte[]{});
    }

    /**
     * Returns a {@code BinaryArray} wrapping an input array of bytes
     *
     * @param in the array of bytes to wrap
     * @return a new {@code BinaryArray}, its content might be empty
     */
    public static @NotNull BinaryArray forArray(byte[] in){
        return new BinaryArray(in);
    }

    /**
     * Returns a {@code BinaryArray} wrapping a single byte
     *
     * @param in the byte to wrap
     * @return a new non empty {@code BinaryArray}
     */
    public static @NotNull BinaryArray singleton(byte in){
        return new BinaryArray(new byte[]{in});
    }

    /**
     * Returns a {@code BinaryArray} wrapping the array of bytes representing a UTF-8 string
     *
     * @param in the String to wrap
     * @return a new {@code BinaryArray}, its content might be empty
     */
    public static @NotNull BinaryArray forString(@NotNull String in){
        return forArray(in.getBytes());
    }

    /**
     * Returns a {@code BinaryArray} wrapping the array of bytes representing a UTF-8 string encoded using the Base64 format
     *
     * @param input the Base64 encoded String to wrap
     * @return a new {@code BinaryArray}, its content might be empty
     */
    public static @NotNull BinaryArray forBase64(@NotNull String input){
        return forArray(Base64.getDecoder().decode(input));
    }

    /**
     * Returns a {@code BinaryArray} wrapping the array of pseudo random bytes with length equal to {@param length}
     *
     * @param length the length of the array to wrap
     * @return a new {@code BinaryArray}, its content can be empty only if {@param length} equals 0
     */
    public static @NotNull BinaryArray random(int length){
        final var result = new byte[length];
        new SecureRandom().nextBytes(result);
        return forArray(result);
    }

    /**
     * Returns a new {@code BinaryArray} wrapping a new byte array
     * Said new byte array is obtained by slicing the byte array that this object wraps from index 0, inclusive, to {@param end}, exclusive
     *
     * @param end the exclusive index used to slice this object's bytes array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public @NotNull BinaryArray cut(int end){
        return slice(0, end);
    }

    public @NotNull BinaryArray slice(int start){
        return slice(start, data.length);
    }

    public @NotNull Pair<BinaryArray, BinaryArray> split(int split){
        return new Pair<>(cut(split), slice(split + 1));
    }

    public @NotNull BinaryArray slice(int start, int end){
        return forArray(Arrays.copyOfRange(data, start, end));
    }

    public @NotNull BinaryArray merged(@NotNull BinaryArray array){
        var result = Arrays.copyOf(data, size() + array.size());
        System.arraycopy(array.data, 0, result, size(), array.size());
        return forArray(result);
    }

    public @NotNull Optional<Integer> indexOf(char character){
        return IntStream.range(0, size()).filter(index -> data[index] == character).boxed().findAny();
    }

    public byte at(int index){
        return data[index];
    }

    public int size(){
        return data.length;
    }

    public @NotNull ByteBuffer toBuffer() {
        return ByteBuffer.wrap(data);
    }

    public @NotNull String toHex(){
        return DatatypeConverter.printHexBinary(data);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof BinaryArray that && Arrays.equals(data, that.data);
    }

    @Override
    public @NotNull String toString() {
        return new String(data(), StandardCharsets.UTF_8);
    }
}
