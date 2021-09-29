import it.auties.whatsapp4j.beta.serialization.StanzaEncoder;
import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.IntStream;

public class Buffer {
    public static void main(String[] args) {
      try {
          var stanzaEncoder = new StanzaEncoder();
          var keepAlive = new Node("iq", orderedAttrs(), new Node("ping", Map.of(), null));
          var result = stanzaEncoder.encodeStanza(keepAlive);
          var beta = IntStream.range(0, result.length)
                  .map(index -> Byte.toUnsignedInt(result[index]))
                  .toArray();
          System.out.println(Arrays.toString(beta));
      }catch (Throwable ex){
          ex.printStackTrace();
      }
    }

    private static LinkedHashMap<String, Object> orderedAttrs() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", "mario");
        map.put("to", Jid.WHATSAPP_SERVER);
        map.put("type", "get");
        map.put("xmlns", "w:p");
        return map;
    }
}
