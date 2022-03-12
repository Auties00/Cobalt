package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.request.Node;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static it.auties.whatsapp.model.request.Node.withAttributes;
import static java.util.Map.of;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class LTHashState {
    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private long version;

    @JsonProperty("hash")
    private byte[] hash;

    @JsonProperty("values")
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
        return withAttributes("internal",
                of("name", name, "version", String.valueOf(version), "return_snapshot", Boolean.toString(version == 0)));
    }

    public LTHashState copy(){
        var newHash = Arrays.copyOf(hash, hash.length);
        var newData = new HashMap<>(indexValueMap);
        return new LTHashState(name, version, newHash, newData);
    }
}