package it.auties.github.dev;

import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.protobuf.sync.MutationSync;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log
public class LTHashStateTest {
    @Test
    public void test(){
        var ltHash = new LTHash("Mario".getBytes(UTF_8));
        ltHash.mix("Set".getBytes(UTF_8), "value".getBytes(UTF_8), MutationSync.Operation.SET);
        System.out.println(HexFormat.of().formatHex(ltHash.finish()));
    }
}
