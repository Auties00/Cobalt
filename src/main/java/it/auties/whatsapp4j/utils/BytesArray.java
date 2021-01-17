package it.auties.whatsapp4j.utils;

import jakarta.xml.bind.DatatypeConverter;
import org.glassfish.grizzly.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.IntStream;

public record BytesArray(byte[] data) {
    public static @NotNull BytesArray forArray(byte[] in){
        return new BytesArray(in);
    }

    public static @NotNull BytesArray forBase64(String input){
        return forArray(Base64.getDecoder().decode(input));
    }

    public static @NotNull BytesArray random(int length){
        final var result = new byte[length];
        new SecureRandom().nextBytes(result);
        return forArray(result);
    }

    public @NotNull BytesArray cut(int end){
        return slice(0, end);
    }

    public @NotNull BytesArray slice(int start){
        return slice(start, data.length);
    }

    public @NotNull Pair<BytesArray, BytesArray> split(int split){
        return new Pair<>(cut(split), slice(split + 1));
    }

    public @NotNull BytesArray slice(int start, int end){
        return forArray(Arrays.copyOfRange(data, start, end));
    }

    public @NotNull BytesArray merged(@NotNull BytesArray array){
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

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof BytesArray that && Arrays.equals(data, that.data);
    }

    @Override
    public String toString() {
        return new String(data());
    }
}
