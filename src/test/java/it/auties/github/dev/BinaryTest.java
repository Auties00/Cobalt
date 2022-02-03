package it.auties.github.dev;

import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.socket.Node;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.Map;

@Log
public class BinaryTest {
    @Test
    public void test(){
        var node = Node.withAttributes("ack",
                Map.of("class", "receipt", "to", "393495089819@s.whatsapp.net", "id", "BB723359FF0367667F4606F9A759AD7B"));
        var encoded = new BinaryEncoder()
                .encode(node);
        var decoded = new BinaryDecoder()
                .decode(encoded);
        log.info("Encoded: " + node.toString());
        log.info(decoded.toString());
    }
}
