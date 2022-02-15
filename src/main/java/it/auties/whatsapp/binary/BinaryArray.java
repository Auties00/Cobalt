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


public record BinaryArray(byte @NonNull [] data) {
    
    public static BinaryArray empty() {
        return of(new byte[0]);
    }

    
    public static BinaryArray of(byte... input) {
        return new BinaryArray(input);
    }

    
    public static BinaryArray of(byte[]... input) {
        return Arrays.stream(input)
                .map(BinaryArray::of)
                .reduce(empty(), BinaryArray::append);
    }

    
    public static BinaryArray of(@NonNull String input) {
        return of(input.getBytes(StandardCharsets.UTF_8));
    }

    
    public static BinaryArray ofBase64(@NonNull String input) {
        return of(Base64.getDecoder().decode(input));
    }

    
    public static BinaryArray ofHex(@NonNull String input) {
        return of(Hex.decodeStrict(input));
    }

    
    public static BinaryArray random(int length) {
        var result = allocate(length);
        new SecureRandom().nextBytes(result.data());
        return result;
    }

    
    public static BinaryArray allocate(int length) {
        return of(new byte[length]);
    }

    
    public BinaryArray cut(int end) {
        return slice(0, end);
    }

    
    public BinaryArray slice(int start) {
        return slice(start, data.length);
    }

    
    public BinaryArray slice(int start, int end) {
        return of(copyOfRange(data, start >= 0 ? start : size() + start, end >= 0 ? end : size() + end));
    }

    
    public BinaryArray withFirst(byte value){
        return with(0, value);
    }

    
    public BinaryArray withLast(byte value){
        return with(size() - 1, value);
    }

    
    public BinaryArray with(int index, byte value){
        Validate.isTrue(index < size(),
                "Cannot set byte at index %s: maximum capacity is %s",
                index, size());
        var result = copyOf(data, data.length);
        result[index] = value;
        return of(result);
    }

    
    public BinaryArray append(BinaryArray array) {
        if(array == null){
            return this;
        }

        var result = copyOf(data, size() + array.size());
        arraycopy(array.data, 0, result, size(), array.size());
        return of(result);
    }

    
    public BinaryArray append(byte... array) {
        if(array == null){
            return this;
        }

        var result = copyOf(data, size() + array.length);
        arraycopy(array, 0, result, size(), array.length);
        return of(result);
    }

    
    public BinaryArray fill(byte value) {
        return fill(value, size());
    }

    
    public BinaryArray fill(byte value, int length) {
        var result = new byte[size()];
        for(var i = 0; i < size(); i++){
            var entry = at(i);
            result[i] = i < length && entry == 0 ? value : entry;
        }

        return of(result);
    }

    
    public Optional<Integer> indexOf(char character) {
        return IntStream.range(0, size()).filter(index -> data[index] == character).boxed().findFirst();
    }

    
    public byte at(int index) {
        return data[index];
    }

    
    public BinaryArray assertSize(int size) {
        Validate.isTrue(size() == size,
                "Invalid size: expected %s, got %s",
                size, size());
        return this;
    }

    
    public int size() {
        return data.length;
    }

    
    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(data);
    }

    
    public ByteArrayOutputStream toOutputStream() {
        try {
            var stream = new ByteArrayOutputStream(size());
            stream.write(data);
            return stream;
        }catch (IOException exception){
            throw new UnsupportedOperationException("Cannot transform BinaryArray to ByteArrayOutputStream", exception);
        }
    }

    
    public String toHex() {
        return Hex.toHexString(data);
    }

    
    public int toInt() {
        Validate.isTrue(size() < 5, "Cannot convert BinaryArray to int, overflow: %s", size());
        var value = 0;
        for (var i = 0; i < size(); i++) {
            value = (value << 8) | (Byte.toUnsignedInt(at(i)));
        }

        return value;
    }

    
    public String toBase64(){
        return Base64.getEncoder().encodeToString(data());
    }

    
    @Override
    public String toString() {
        return new String(data(), StandardCharsets.UTF_8);
    }

    
    @Override
    public boolean equals(Object other) {
        return (other instanceof byte[] bytes && Arrays.equals(data, bytes))
                || (other instanceof BinaryArray that && Arrays.equals(data, that.data));
    }
}