package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.binary.Sync;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.auties.whatsapp.model.request.Node.withAttributes;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class LTHashState {
    private String name;

    private long version;

    private byte[] hash;

    private Map<String, byte[]> indexValueMap;

    public LTHashState(Sync name) {
        this(name, 0);
    }

    public LTHashState(Sync name, long version) {
        this.name = Objects.toString(name);
        this.version = version;
        this.hash = new byte[128];
        this.indexValueMap = new HashMap<>();
    }

    public Node toNode() {
        var attributes = Attributes.empty()
                .put("name", name)
                .put("version", version)
                .put("return_snapshot", version == 0)
                .map();
        return withAttributes("collection", attributes);
    }

    public LTHashState copy() {
        var newHash = Arrays.copyOf(hash, hash.length);
        var newData = new HashMap<>(indexValueMap);
        return new LTHashState(name, version, newHash, newData);
    }
}