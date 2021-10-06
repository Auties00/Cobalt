import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.utils.Jid;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Testing {
    public void main(String[] args) {
        var a = "19";
        for(var x = 0; x < a.length(); x++){
            System.out.println(Character.codePointAt(a, x));
        }
    }

    private Map<String, Object> orderedMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", "mario");
        map.put("to", Jid.WHATSAPP_SERVER);
        map.put("type", "get");
        map.put("xmlns", "w:p");
        return map;
    }

    private void printKeepAlive(Node keepAlive) {
        var encoded = new BinaryEncoder().encode(keepAlive);
        System.out.printf("Keep alive: %s%n", Arrays.toString(BinaryArray.forArray(encoded).toUnsigned()));
    }
}
