package it.auties.whatsapp.protobuf.sync;

import it.auties.whatsapp.socket.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static it.auties.whatsapp.socket.Node.withChildren;
import static java.util.Map.of;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class LTHashState {
    private long version;
    private byte[] hash;
    private Map<String, byte[]> indexValueMap;

    public LTHashState(){
        this(0);
    }

    public LTHashState(long version){
        this.version = version;
        this.hash = new byte[128];
        this.indexValueMap = new HashMap<>();
    }

    public Node toNode(String name){
        return withChildren("collection",
                of("name", name, "version", String.valueOf(version), "return_snapshot", Boolean.toString(version != 0)));
    }

    public LTHashState copy(){
        var newHash = Arrays.copyOf(hash, hash.length);
        var newData = new HashMap<>(indexValueMap);
        return new LTHashState(version, newHash, newData);
    }
}