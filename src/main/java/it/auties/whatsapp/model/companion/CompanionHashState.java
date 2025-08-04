package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.auties.whatsapp.model.node.Node.of;

@ProtobufMessage
public final class CompanionHashState {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PatchType type;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long version;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] hash;

    @ProtobufProperty(index = 4, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.BYTES)
    Map<Integer, byte[]> indexValueMap;

    public CompanionHashState(PatchType type) {
        this(type, 0);
    }

    public CompanionHashState(PatchType type, long version) {
        this.type = type;
        this.version = version;
        this.hash = new byte[128];
        this.indexValueMap = new HashMap<>();
    }

    public CompanionHashState(PatchType type, long version, byte[] hash, Map<Integer, byte[]> indexValueMap) {
        this.type = type;
        this.version = version;
        this.hash = hash;
        this.indexValueMap = indexValueMap;
    }

    private static boolean checkIndexEntryEquality(CompanionHashState that, int thisKey, byte[] thisValue) {
        var thatValue = that.indexValueMap().get(thisKey);
        return thatValue != null && Arrays.equals(thatValue, thisValue);
    }

    public Node toNode() {
        var attributes = Attributes.of()
                .put("name", type)
                .put("version", version)
                .put("return_snapshot", version == 0)
                .toMap();
        return of("collection", attributes);
    }

    public CompanionHashState copy() {
        return new CompanionHashState(type, version, Arrays.copyOf(hash, hash.length), new HashMap<>(indexValueMap));
    }

    private boolean checkIndexEquality(CompanionHashState that) {
        if (indexValueMap.size() != that.indexValueMap().size()) {
            return false;
        }
        return indexValueMap().entrySet()
                .stream()
                .allMatch(entry -> checkIndexEntryEquality(that, entry.getKey(), entry.getValue()));
    }

    public PatchType type() {
        return this.type;
    }

    public long version() {
        return this.version;
    }

    public byte[] hash() {
        return this.hash;
    }

    public Map<Integer, byte[]> indexValueMap() {
        return this.indexValueMap;
    }

    public void setType(PatchType name) {
        this.type = name;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public void setIndexValueMap(Map<Integer, byte[]> indexValueMap) {
        this.indexValueMap = indexValueMap;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof CompanionHashState that
                && this.version == that.version()
                && this.type == that.type()
                && Arrays.equals(this.hash, that.hash()) && checkIndexEquality(that);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(type, version, indexValueMap);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}