package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.binary.PatchType;
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
import java.util.stream.Collectors;

import static it.auties.whatsapp.model.request.Node.withAttributes;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class LTHashState {
    private PatchType name;

    private long version;

    private byte[] hash;

    private Map<String, byte[]> indexValueMap;

    public LTHashState(PatchType name) {
        this(name, 0);
    }

    public LTHashState(PatchType name, long version) {
        this.name = name;
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
        var newData = indexValueMap.entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new LTHashState(name, version, newHash, newData);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LTHashState that
                && this.version == that.version()
                && this.name == that.name()
                && Arrays.equals(this.hash, that.hash())
                && checkIndexEquality(that);
    }

    private boolean checkIndexEquality(LTHashState that) {
        if (indexValueMap.size() != that.indexValueMap()
                .size()) {
            return false;
        }

        return indexValueMap().entrySet()
                .stream()
                .allMatch(entry -> checkIndexEntryEquality(that, entry.getKey(), entry.getValue()));
    }

    private static boolean checkIndexEntryEquality(LTHashState that, String thisKey, byte[] thisValue) {
        var thatValue = that.indexValueMap()
                .get(thisKey);
        return thatValue != null && Arrays.equals(thatValue, thisValue);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(name, version, indexValueMap);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}