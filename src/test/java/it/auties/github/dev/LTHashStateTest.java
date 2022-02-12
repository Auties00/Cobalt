package it.auties.github.dev;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.sync.MutationSync;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log
public class LTHashStateTest {
    @Test
    public void test(){
        System.out.println(HexFormat.of().formatHex(pad("mario".getBytes(UTF_8))));
    }

    public byte[] pad(byte[] bytes){
        var padRandomByte = 1 + (15 & 5);
        var padding = BinaryArray.allocate(padRandomByte)
                .fill((byte) padRandomByte)
                .data();
        return BinaryArray.of(bytes)
                .append(padding)
                .data();
    }
}
