package it.auties.whatsapp.binary;

import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

/**
 * A utility class that wraps an array of bytes
 */
public record BinaryArray(byte @NonNull [] data) {
    /**
     * Constructs a new empty {@code BinaryArray}
     *
     * @return a new {@code BinaryArray} wrapping an empty bytes array
     */
    public static BinaryArray empty() {
        return of();
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping {@param input}
     *
     * @param input the array of bytes to wrap
     * @return a new {@code BinaryArray} wrapping {@code input}
     */
    public static BinaryArray of(byte... input) {
        return new BinaryArray(input);
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping the array of bytes representing a UTF-8 string
     *
     * @param input the String to wrap
     * @return a new {@code BinaryArray} wrapping {@code input}
     */
    public static BinaryArray of(@NonNull String input) {
        return of(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constructs a {@code BinaryArray} wrapping the array of bytes representing a Base64 encoded String in UTF-8 format
     *
     * @param input the Base64 encoded String to wrap
     * @return a new {@code BinaryArray} wrapping {@code input}
     */
    public static BinaryArray ofBase64(@NonNull String input) {
        return of(Base64.getDecoder().decode(input));
    }

    /**
     * Constructs a {@code BinaryArray} wrapping the array of bytes representing a Hex encoded string
     *
     * @param input the Hex encoded String to wrap
     * @return a new {@code BinaryArray} wrapping {@code input}
     */
    public static BinaryArray ofHex(@NonNull String input) {
        return of(Hex.decodeStrict(input));
    }

    /**
     * Constructs a {@code BinaryArray} wrapping a generated array of {@code length} pseudo random bytes
     *
     * @param length the length of the array to generate and wrap
     * @return a new {@code BinaryArray} of length {@code length}
     */
    public static BinaryArray random(int length) {
        var result = allocate(length);
        new SecureRandom().nextBytes(result.data());
        return result;
    }

    /**
     * Constructs a {@code BinaryArray} wrapping an array of a fixed size
     *
     * @param length the length of the array to generate and wrap
     * @return a new {@code BinaryArray} of length {@code length}
     */
    public static BinaryArray allocate(int length) {
        return of(new byte[length]);
    }

    /**
     * Constructs a new {@code BinaryArray} wrapping this object's bytes array sliced from
     * 0, inclusive
     * {@param end}, exclusive
     *
     * @param end the exclusive index used to slice this object's bytes array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public BinaryArray cut(int end) {
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
    public BinaryArray slice(int start) {
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
    public BinaryArray slice(int start, int end) {
        return of(copyOfRange(data, start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    /**
     * Constructs a new {@code BinaryArray} by setting {@code value} at the starting position
     *
     * @param value the value to insert
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by setting an input value at the starting position
     */
    public BinaryArray withFirst(byte value){
        return with(0, value);
    }

    /**
     * Constructs a new {@code BinaryArray} by setting {@code value} at the ending position
     *
     * @param value the value to insert
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by setting an input value at the ending position
     */
    public BinaryArray withLast(byte value){
        return with(size() - 1, value);
    }

    /**
     * Constructs a new {@code BinaryArray} by setting {@code value} at {@code index}
     *
     * @param index the index where the new value should be placed
     * @param value the value to insert
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by setting an input value at the input index
     */
    public BinaryArray with(int index, byte value){
        Validate.isTrue(index < size(),
                "Cannot set byte at index %s: maximum capacity is %s",
                index, size());
        var result = copyOf(data, data.length);
        result[index] = value;
        return of(result);
    }

    /**
     * Constructs a new {@code BinaryArray} by concatenating this object and {@param array}
     *
     * @param array the {@code BinaryArray} to concatenate
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by concatenating this object's bytes array and {@param array}'s bytes array
     */
    public BinaryArray append(BinaryArray array) {
        if(array == null){
            return this;
        }

        var result = copyOf(data, size() + array.size());
        arraycopy(array.data, 0, result, size(), array.size());
        return of(result);
    }

    /**
     * Constructs a new {@code BinaryArray} by concatenating this object and {@param array}
     *
     * @param array the {@code byte[]} to concatenate
     * @return a new {@code BinaryArray} wrapping a bytes array obtained by concatenating this object's bytes array and {@param array}
     */
    public BinaryArray append(byte... array) {
        if(array == null){
            return this;
        }

        var result = copyOf(data, size() + array.length);
        arraycopy(array, 0, result, size(), array.length);
        return of(result);
    }

    /**
     * Constructs a new {@code BinaryArray} filled with the values provided.
     * This operation is only applied if the original value was unset.
     *
     * @param value  the value to use to fill the array
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public BinaryArray fill(byte value) {
        return fill(value, size());
    }

    /**
     * Constructs a new {@code BinaryArray} filled with the values provided.
     * This operation is only applied if the original value was unset.
     * This operation is applied on the length provided.
     *
     * @param value  the value to use to fill the array
     * @param length the length where the fill should be applied
     * @return a new {@code BinaryArray} with the above characteristics
     */
    public BinaryArray fill(byte value, int length) {
        var result = new byte[size()];
        for(var i = 0; i < size(); i++){
            var entry = at(i);
            result[i] = i < length && entry == 0 ? value : entry;
        }

        return of(result);
    }

    /**
     * Returns the index within this object's bytes array of the first occurrence of a byte that matches {@param character}
     * If this condition is met, a non-empty Optional wrapping said index is returned
     * Otherwise, an empty Optional is returned
     *
     * @param character the character to search
     * @return an Optional wrapping an int with the above characteristics
     */
    public Optional<Integer> indexOf(char character) {
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
     * Asserts that the size of this array is equal to the one provided
     *
     * @param size the expected size
     * @throws IllegalArgumentException if the size of this array doesn't match the provided one
     * @return this array
     */
    public BinaryArray assertSize(int size) {
        Validate.isTrue(size() == size,
                "Invalid size: expected %s, got %s",
                size, size());
        return this;
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
     * Constructs a new ByteBuffer from this object's array of bytes
     *
     * @return an ByteBuffer with the above characteristics
     */
    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(data);
    }

    /**
     * Constructs a new OutputStream from this object's array of bytes
     *
     * @return an ByteBuffer with the above characteristics
     */
    public ByteArrayOutputStream toOutputStream() {
        try {
            var stream = new ByteArrayOutputStream(size());
            stream.write(data);
            return stream;
        }catch (IOException exception){
            throw new UnsupportedOperationException("Cannot transform BinaryArray to ByteArrayOutputStream", exception);
        }
    }

    /**
     * Constructs a new hex from this object's array of bytes
     *
     * @return a String with the above characteristics
     */
    public String toHex() {
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
     * Constructs a new base 64 encoded String from this object's array of bytes
     *
     * @return a String with the above characteristics
     */
    public String toBase64(){
        return Base64.getEncoder().encodeToString(data());
    }

    /**
     * Constructs a UTF-8 encoded String using this object's array of bytes
     *
     * @return a String with the above characteristics
     */
    @Override
    public String toString() {
        return new String(data(), StandardCharsets.UTF_8);
    }

    /**
     * Checks if this object and {@param other} are equal
     *
     * @param other other
     * @return true if {@param other} is an instance of {@code BinaryArray} and/or if they wrap two arrays considered equal
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof byte[] bytes && Arrays.equals(data, bytes))
                || (other instanceof BinaryArray that && Arrays.equals(data, that.data));
    }
}