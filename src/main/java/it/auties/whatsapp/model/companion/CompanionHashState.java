package it.auties.whatsapp.model.companion;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.auties.whatsapp.model.node.Node.of;

public final class CompanionHashState {
    private PatchType name;

    private long version;

    private byte[] hash;

    private Map<String, byte[]> indexValueMap;

    public CompanionHashState(PatchType name) {
        this(name, 0);
    }

    public CompanionHashState(PatchType name, long version) {
        this.name = name;
        this.version = version;
        this.hash = new byte[128];
        this.indexValueMap = new HashMap<>();
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CompanionHashState(PatchType name, long version, byte[] hash, Map<String, byte[]> indexValueMap) {
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

    public CompanionHashState copy() {
        return new CompanionHashState(name, version, Arrays.copyOf(hash, hash.length), new HashMap<>(indexValueMap));
    }

    private boolean checkIndexEquality(CompanionHashState that) {
        if (indexValueMap.size() != that.indexValueMap().size()) {
            return false;
        }
        return indexValueMap().entrySet()
                .stream()
                .allMatch(entry -> checkIndexEntryEquality(that, entry.getKey(), entry.getValue()));
    }

    private static boolean checkIndexEntryEquality(CompanionHashState that, String thisKey, byte[] thisValue) {
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

    public CompanionHashState name(PatchType name) {
        this.name = name;
        return this;
    }

    public CompanionHashState version(long version) {
        this.version = version;
        return this;
    }

    public CompanionHashState hash(byte[] hash) {
        this.hash = hash;
        return this;
    }

    public CompanionHashState indexValueMap(Map<String, byte[]> indexValueMap) {
        this.indexValueMap = indexValueMap;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof CompanionHashState that
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