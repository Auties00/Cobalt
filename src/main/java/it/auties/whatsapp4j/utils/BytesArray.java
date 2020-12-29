package it.auties.whatsapp4j.utils;

import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Base64;

public record BytesArray(byte[] data) {
    public static @NotNull BytesArray allocate(int size){
        return new BytesArray(new byte[size]);
    }

    public static @NotNull BytesArray forArray(byte[] in){
        return new BytesArray(in);
    }

    public static @NotNull BytesArray forBase64(String input){
        return new BytesArray(Base64.getDecoder().decode(input));
    }

    public @NotNull BytesArray cut(int end){
        return slice(0, end);
    }

    public @NotNull BytesArray slice(int start){
        return slice(start, data.length);
    }

    public @NotNull BytesArray slice(int start, int end){
        return forArray(Arrays.copyOfRange(data, start, end));
    }

    public @NotNull BytesArray join(@NotNull BytesArray array){
        return forArray(ArrayUtils.addAll(data(), array.data()));
    }

    public @NotNull BytesArray add(byte data){
        return forArray(ArrayUtils.add(data(), data));
    }

    public int size(){
        return data().length;
    }

    @Override
    public boolean equals(@Nullable Object o) { return o instanceof BytesArray that && Arrays.equals(data, that.data); }

    @Override
    public String toString() {
        return DatatypeConverter.printHexBinary(data());
    }
}
