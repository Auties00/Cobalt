import it.auties.whatsapp4j.beta.serialization.Binary;
import it.auties.whatsapp4j.beta.serialization.StanzaEncoder;
import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import org.bouncycastle.util.encoders.Hex;

import java.util.*;
import java.util.stream.IntStream;

public class Buffer {
    public static void main(String[] args) {
        var node = new Node("iq", Map.of("id", "123"), List.of(new Node("ping", Map.of("id", "123"), List.of(new Node("iq", Map.of("id", "123"), null)))));
        var encoded = new StanzaEncoder().encodeStanza(node);
        System.out.println(Arrays.toString(encoded));
        System.out.println(Arrays.toString(BinaryArray.forArray(encoded).toUnsigned()));
        System.out.println(HexFormat.of().formatHex(encoded));
        var decoded = new StanzaEncoder().decodeStanza(new StanzaEncoder().unpackStanza(encoded));
        System.out.println(decoded);
    }
}
