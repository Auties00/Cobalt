package it.auties.whatsapp.utils;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class Chunks {
    public final int SECRET_MAX_SIZE = 32000;

    public byte[][] partition(byte[] arrayToSplit){
        var rest = arrayToSplit.length % SECRET_MAX_SIZE;
        var chunks = arrayToSplit.length / SECRET_MAX_SIZE + (rest > 0 ? 1 : 0);
        var arrays = new byte[chunks][];
        for(int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++) {
            arrays[i] = Arrays.copyOfRange(arrayToSplit, i * SECRET_MAX_SIZE, i * SECRET_MAX_SIZE + SECRET_MAX_SIZE);
        }

        if(rest > 0){
            arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * SECRET_MAX_SIZE, (chunks - 1) * SECRET_MAX_SIZE + rest);
        }

        return arrays;
    }
}
