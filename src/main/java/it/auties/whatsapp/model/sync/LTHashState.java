package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.auties.whatsapp.model.node.Node.of;

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

    public LTHashState(PatchType name, long version, byte[] hash, Map<String, byte[]> indexValueMap) {
        this.name = name;
        this.version = version;
        this.hash = hash;
        this.indexValueMap = indexValueMap;
    }

    public Node toNode() {
        var attributes = Attributes.of()
                .put("name", name)
                .put("version", version)
                .put("return_snapshot", version == 0)
                .toMap();
        return of("collection", attributes);
    }

    public LTHashState copy() {
        return new LTHashState(name, version, Arrays.copyOf(hash, hash.length), new HashMap<>(indexValueMap));
    }

    private boolean checkIndexEquality(LTHashState that) {
        if (indexValueMap.size() != that.indexValueMap().size()) {
            return false;
        }
        return indexValueMap().entrySet()
                .stream()
                .allMatch(entry -> checkIndexEntryEquality(that, entry.getKey(), entry.getValue()));
    }

    private static boolean checkIndexEntryEquality(LTHashState that, String thisKey, byte[] thisValue) {
        var thatValue = that.indexValueMap().get(thisKey);
        return thatValue != null && Arrays.equals(thatValue, thisValue);
    }

    public PatchType name() {
        return this.name;
    }

    public long version() {
        return this.version;
    }

    public byte[] hash() {
        return this.hash;
    }

    public Map<String, byte[]> indexValueMap() {
        return this.indexValueMap;
    }

    public LTHashState name(PatchType name) {
        this.name = name;
        return this;
    }

    public LTHashState version(long version) {
        this.version = version;
        return this;
    }

    public LTHashState hash(byte[] hash) {
        this.hash = hash;
        return this;
    }

    public LTHashState indexValueMap(Map<String, byte[]> indexValueMap) {
        this.indexValueMap = indexValueMap;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof LTHashState that
                && this.version == that.version()
                && this.name == that.name()
                && Arrays.equals(this.hash, that.hash()) && checkIndexEquality(that);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(name, version, indexValueMap);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}