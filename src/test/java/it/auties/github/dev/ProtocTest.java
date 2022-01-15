package it.auties.github.dev;

import it.auties.whatsapp.protobuf.signal.sender.SenderKeyMessage;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

public class ProtocTest {
    @Test
    public void test() throws Exception {
        var data = HexFormat.of()
                .parseHex("3308db8ce3da0110011a10fdc9049c0a72b48c8d19ee45312ed1502c2c55785d469ec582b790f2c675d685c2399ed9a11f9b9445234e2914c22dc259e02a1558f50cd940fb1e5de841cc57791f76fdc5af5c33ca7902cbc27bdf0d");
        var beta = SenderKeyMessage.ofEncoded(data);

        System.out.println(HexFormat.of().formatHex(beta.serialized()));
    }
}
