import it.auties.whatsapp4j.binary.BinaryDecoder;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryUnpack;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import it.auties.whatsapp4j.utils.Jid;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class Testing {
    public void main(String[] args) {
        var iq = new Node(
                "iq",
                Map.of(
                        "id", "939139193",
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "result"
                ),
                List.of(
                        new Node(
                                "pair-device-sign",
                                Map.of(),
                                List.of(
                                        new Node(
                                                "device-identity",
                                                Map.of("key-index", 21),
                                                CypherUtils.randomKeyPair().getPrivate().getEncoded()
                                        )
                                )
                        )
                )
        );
        System.out.println(iq);
        System.out.println(new BinaryDecoder().decode(BinaryUnpack.unpack(new BinaryEncoder().encode(iq))));
    }
}
