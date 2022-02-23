package it.auties.whatsapp.protobuf.sync;

import it.auties.whatsapp.socket.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static it.auties.whatsapp.socket.Node.withAttributes;
import static it.auties.whatsapp.socket.Node.withChildren;
import static java.util.Map.of;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class LTHashState {
    private String name;
    private long version;
    private byte[] hash;
    private Map<String, byte[]> indexValueMap;

    public LTHashState(String name){
        this(name, 0);
    }

    public LTHashState(long version){
        this(null, version);
    }

    public LTHashState(String name, long version){
        this.name = name;
        this.version = version;
        this.hash = new byte[128];
        this.indexValueMap = new HashMap<>();
    }

    public Node toNode(){
        return withAttributes("collection",
                of("name", name, "version", String.valueOf(version), "return_snapshot", Boolean.toString(version == 0)));
    }

    public LTHashState copy(){
        var newHash = Arrays.copyOf(hash, hash.length);
        var newData = new HashMap<>(indexValueMap);
        return new LTHashState(name, version, newHash, newData);
    }
}