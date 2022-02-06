package it.auties.github.dev;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.LTHash;
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
        var random = BinaryArray.random(32);
        System.out.println(Arrays.toString(random.data()));
        System.out.println(Arrays.toString(random.slice(-16).data()));
    }
}
