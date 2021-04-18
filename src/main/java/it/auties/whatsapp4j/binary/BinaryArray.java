package it.auties.whatsapp4j.binary;

import it.auties.whatsapp4j.response.model.Pair;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.DatatypeConverter;
import lombok.*;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;
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
@AllArgsConstructor
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class BinaryArray {
    private final byte[] data;


    /**
     * Constructs a new empty {@link BinaryArray}
     *
     * @return a new {@link BinaryArray}
     */
    public static @NotNull BinaryArray empty() {
        return forArray(new byte[0]);
    }

    /**
     * Constructs a new {@link BinaryArray} that wraps an array of bytes
     *
     * @param in the array of bytes to wrap
     * @return a new {@link BinaryArray}
     */
    public static @NotNull BinaryArray forArray(byte[] in) {
        return new BinaryArray(in);
    }

    /**
     * Constructs a new non empty {@link BinaryArray} that wraps a byte
     *
     * @param in the byte to wrap
     * @return a new non empty {@link BinaryArray}
     */
    public static @NotNull BinaryArray singleton(byte in) {
        return new BinaryArray(new byte[]{in});
    }

    /**
     * Constructs a new {@link BinaryArray} that wraps the array of bytes obtained from a UTF-8 encoded String
     *
     * @param in the String to wrap
     * @return a new {@link BinaryArray}
     */
    public static @NotNull BinaryArray forString(@NotNull String in) {
        return forArray(in.getBytes());
    }

    /**
     * Constructs a {@link BinaryArray} that wraps the array of bytes obtained from a Base64 encoded String
     *
     * @param input the Base64 encoded String to wrap
     * @return a new {@link BinaryArray}
     */
    public static @NotNull BinaryArray forBase64(@NotNull String input) {
        return forArray(Base64.getDecoder().decode(input));
    }

    /**
     * Constructs a {@link BinaryArray} that wraps an array of pseudorandom bytes
     *
     * @param length the length of the array to generate and wrap
     * @return a new {@link BinaryArray}
     */
    public static @NotNull BinaryArray random(int length) {
        final var result = new byte[length];
        new SecureRandom().nextBytes(result);
        return forArray(result);
    }

    /**
     * Constructs a new {@link BinaryArray} whose content is a subsequence of this object.
     * The content of the new {@link BinaryArray} will start at position 0 and will contain {@code length} elements.
     *
     * @param length the length of the new {@link BinaryArray}
     * @return a new {@link BinaryArray}
     */
    public @NotNull BinaryArray cut(int length) {
        return slice(0, length);
    }

    /**
     * Constructs a new {@link BinaryArray} whose content is a subsequence of this object.
     * The content of the new {@link BinaryArray} will start at position {@code start} and will contain {@code size() - start} elements.
     *
     * @param start the inclusive index to slice this object
     * @return a new {@link BinaryArray}
     */
    public @NotNull BinaryArray slice(int start) {
        return slice(start, size());
    }

    /**
     * Constructs a new {@link BinaryArray} whose content is a subsequence of this object.
     * The content of the new {@link BinaryArray} will start at position {@code start} and will contain {@code end - start} elements.
     *
     * @param start the inclusive starting index to slice this object
     * @param end the exclusive ending index to slice this object
     * @return a new {@link BinaryArray}
     */
    public @NotNull BinaryArray slice(int start, int end) {
        return forArray(Arrays.copyOfRange(data, start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    /**
     * Constructs a {@link Pair} of two {@link BinaryArray} obtained by splitting this object at {@code split}.
     * The first {@link BinaryArray} will start at index 0 and end at index {@code split - 1}.
     * The second {@link BinaryArray} will start at index {@code split + 1} and end at index {@code size() - 1}.
     *
     * @param split the index to split this object's array
     * @return a new {@link Pair}
     */
    public @NotNull Pair<BinaryArray, BinaryArray> split(int split) {
        return new Pair<>(cut(split), slice(split + 1));
    }

    /**
     * Constructs a new {@link BinaryArray} obtained by concatenating this object and {@code array}
     *
     * @param array the {@link BinaryArray} to concatenate
     * @return a new {@link BinaryArray}
     */
    public @NotNull BinaryArray merged(@NotNull BinaryArray array) {
        var result = Arrays.copyOf(data, size() + array.size());
        System.arraycopy(array.data, 0, result, size(), array.size());
        return forArray(result);
    }

    /**
     * Returns the index within this object's bytes array of the first occurrence of a byte that matches {@code character}
     * If this condition is met, a non empty Optional wrapping said index is returned
     * Otherwise, an empty Optional is returned
     *
     * @param character the character to search
     * @return a new Optional
     */
    public @NotNull Optional<Integer> indexOf(char character) {
        return IntStream.range(0, size()).filter(index -> data[index] == character).boxed().findFirst();
    }

    /**
     * Returns the byte value at the specified index for this object's bytes array
     *
     * @param index the index, ranges from 0 to size() - 1
     * @return the byte at {@code index}
     */
    public byte at(int index) {
        return data[index];
    }

    /**
     * Returns the size of the array of bytes that this object wraps
     *
     * @return an unsigned int
     */
    public int size() {
        return data.length;
    }

    /**
     * Constructs a new {@link ByteBuffer} from this object's array of bytes
     *
     * @return a new {@link ByteBuffer}
     */
    public @NotNull ByteBuffer toBuffer() {
        return ByteBuffer.wrap(data);
    }

    /**
     * Constructs a new hex String from this object's array of bytes
     *
     * @return a String in hex format
     */
    public @NotNull String toHex() {
        return DatatypeConverter.printHexBinary(data);
    }


}
