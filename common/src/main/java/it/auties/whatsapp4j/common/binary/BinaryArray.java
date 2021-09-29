package it.auties.whatsapp4j.common.binary;

import it.auties.whatsapp4j.common.utils.Validate;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A utility class that wraps an array of bytes
 * It provides an easy interface to modify said data, convert it or generate it
 * This is intended to only be used for WhatsappWeb's WebSocket binary operations
 */
public record BinaryArray(byte[] data) {
    /**
     * Constructs a new empty {@code BinaryArray}
     *
     * @return a new {@code BinaryArray} wrapping an empty bytes array
     */
    public static @NonNull BinaryArray empty() {
        return forArray();
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping {@param in}
     *
     * @param in the array of bytes to wrap
     * @return a new {@code BinaryArray} wrapping {@code in}
     */
    public static @NonNull BinaryArray forArray(byte... in) {
        return new BinaryArray(in);
    }

    /**
     * Constructs a new non-empty {@code BinaryArray}
     *
     * @param in the byte to wrap
     * @return a new non-empty {@code BinaryArray} wrapping a bytes array that only contains {@param in}
     */
    public static @NonNull BinaryArray singleton(byte in) {
        return new BinaryArray(new byte[]{in});
    }

    /**
     * Constructs a new non-empty {@code BinaryArray} of a fixed size representing an integer
     *
     * @param integer the int to wrap
     * @param length  the length of the array
     * @return a new non-empty {@code BinaryArray} wrapping a bytes array that only contains {@param in}
     */
    public static @NonNull BinaryArray forInt(int integer, int length) {
        var result = new byte[length];
        for(var i = length - 1; i >= 0; i--){
            result[i] = (byte) (255 & integer);
            integer >>>= 8;
        }

        return forArray(result);
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping the array of bytes representing a UTF-8 string
     *
     * @param in the String to wrap
     * @return a new {@code BinaryArray} wrapping {@param in}
     */
    public static @NonNull BinaryArray forString(@NonNull String in) {
        return forArray(in.getBytes());
    }

    /**
     * Constructs a {@code BinaryArray} wrapping the array of bytes representing a Base64 encoded String in UTF-8 format
     *
     * @param input the Base64 encoded String to wrap
     * @return a new {@code BinaryArray} wrapping {@param input}
     */
    public static @NonNull BinaryArray forBase64(@NonNull String input) {
        return forArray(Base64.getDecoder().decode(input));
    }

    /**
     * Constructs a {@code BinaryArray} wrapping a generated array of {@param length}pseudo random bytes
     *
     * @param length the length of the array to generate and wrap
     * @return a new {@code BinaryArray} of length {@param length}
     */
    public static @NonNull BinaryArray random(int length) {
        var result = allocate(length);
        new SecureRandom().nextBytes(result.data());
        return result;
    }

    /**
     * Constructs a {@code BinaryArray} wrapping an array of a fixed size
     *
     * @param length the length of the array to generate and wrap
     * @return a new {@code BinaryArray} of length {@param length}
     */
    public static @NonNull BinaryArray allocate(int length) {
        return forArray(new byte[length]);
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping this object's bytes array sliced from
     * 0, inclusive
     * {@param end}, exclusive
     *
     * @param end the exclusive index used to slice this object's bytes array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public @NonNull BinaryArray cut(int end) {
        return slice(0, end);
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping this object's bytes array sliced from
     * {@param start}, inclusive
     * this object's bytes array size, exclusive
     *
     * @param start the inclusive index used to slice this object's bytes array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public @NonNull BinaryArray slice(int start) {
        return slice(start, data.length);
    }

    /**
     * Returns a new {@code BinaryArray} wrapping this object's bytes array sliced from
     * {@param start}, inclusive
     * {@param end}, exclusive
     *
     * @param start the inclusive starting index used to slice this object's bytes array
     * @param end   the exclusive ending index used to slice this object's bytes array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public @NonNull BinaryArray slice(int start, int end) {
        return forArray(Arrays.copyOfRange(data, start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    /**
     * Constructs a new {@code BinaryArray} by concatenating this object and {@param array}
     *
     * @param array the {@code BinaryArray} to concatenate
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by concatenating this object's bytes array and {@param array}'s bytes array
     */
    public @NonNull BinaryArray append(@NonNull BinaryArray array) {
        var result = Arrays.copyOf(data, size() + array.size());
        System.arraycopy(array.data, 0, result, size(), array.size());
        return forArray(result);
    }

    /**
     * Constructs a new {@code BinaryArray} by concatenating this object and {@param array}
     *
     * @param array the {@code byte[]} to concatenate
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by concatenating this object's bytes array and {@param array}
     */
    public @NonNull BinaryArray append(byte @NonNull... array) {
        var result = Arrays.copyOf(data, size() + array.length);
        System.arraycopy(array, 0, result, size(), array.length);
        return forArray(result);
    }

    /**
     * Constructs a new {@code BinaryArray} filled with zero values where needed
     *
     * @param size the new size of the array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public @NonNull BinaryArray fill(int size) {
        return size() >= size ? this : append(allocate(size - size()));
    }

    /**
     * Returns the index within this object's bytes array of the first occurrence of a byte that matches {@param character}
     * If this condition is met, a non-empty Optional wrapping said index is returned
     * Otherwise, an empty Optional is returned
     *
     * @param character the character to search
     * @return an Optional wrapping an int with the above characteristics
     */
    public @NonNull Optional<Integer> indexOf(char character) {
        return IntStream.range(0, size()).filter(index -> data[index] == character).boxed().findFirst();
    }

    /**
     * Returns the byte value at the specified index for this object's bytes array
     *
     * @param index the index, ranges from 0 to size() - 1
     * @return the byte at {@param index}
     */
    public byte at(int index) {
        return data[index];
    }

    /**
     * Returns the size of the array of bytes that this object wraps
     *
     * @return an unsigned int representing the size of the array of bytes that this object wraps
     */
    public int size() {
        return data.length;
    }

    /**
     * Constructs a new BinaryArray from this object's array of bytes and converts them to unsigned ints
     *
     * @return an array of ints with the above characteristics
     */
    public @NonNull int[] toUnsigned() {
        return IntStream.range(0, size())
                .map(index -> Byte.toUnsignedInt(at(index)))
                .toArray();
    }

    /**
     * Constructs a new ByteBuffer from this object's array of bytes
     *
     * @return an ByteBuffer with the above characteristics
     */
    public @NonNull ByteBuffer toBuffer() {
        return ByteBuffer.wrap(data);
    }

    /**
     * Constructs a new hex from this object's array of bytes
     *
     * @return a String with the above characteristics
     */
    public @NonNull String toHex() {
        return Hex.toHexString(data);
    }

    /**
     * Constructs a new int from this object's array of bytes
     *
     * @return an int with the above characteristics
     */
    public int toInt() {
        Validate.isTrue(size() < 5, "Cannot convert BinaryArray to int, overflow: %s", size());
        var value = 0;
        for (var i = 0; i < size(); i++) {
            value = (value << 8) | (Byte.toUnsignedInt(at(i)));
        }

        return value;
    }

    /**
     * Checks if this object and {@param o} are equal
     *
     * @return true if {@param o} is an instance of {@code BinaryArray} and if they wrap two arrays considered equal
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof BinaryArray that && Arrays.equals(data, that.data);
    }

    /**
     * Constructs a UTF-8 encoded String using this object's array of bytes
     *
     * @return a String with the above characteristics
     */
    @Override
    public @NonNull String toString() {
        return new String(data(), StandardCharsets.UTF_8);
    }
}