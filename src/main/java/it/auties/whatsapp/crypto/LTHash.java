package it.auties.whatsapp.crypto;

import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.sync.RecordSync;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

// TODO: Optimize this whole thing including indexValueMap
public final class LTHash {
    private static final int EXPAND_SIZE = 128;
    private static final byte[] SALT = "WhatsApp Patch Integrity".getBytes();

    private final byte[] salt;

    private final byte[] hash;

    private final Map<Integer, byte[]> indexValueMap;

    private final List<byte[]> add, subtract;

    public LTHash(CompanionHashState hash) {
        this.salt = SALT;
        this.hash = hash.hash();
        this.indexValueMap = new HashMap<>(hash.indexValueMap());
        this.add = new ArrayList<>();
        this.subtract = new ArrayList<>();
    }

    public void mix(byte[] indexMac, byte[] valueMac, RecordSync.Operation operation) {
        var hashCode = Arrays.hashCode(indexMac);
        var prevOp = indexValueMap.get(hashCode);
        if (operation == RecordSync.Operation.REMOVE) {
            if (prevOp == null) {
                return;
            }
            indexValueMap.remove(hashCode, prevOp);
        } else {
            add.add(valueMac);
            indexValueMap.put(hashCode, valueMac);
        }
        if (prevOp != null) {
            subtract.add(prevOp);
        }
    }

    public Result finish() {
        var subtracted = perform(hash, false);
        var added = perform(subtracted, true);
        return new Result(added, indexValueMap);
    }

    private byte[] perform(byte[] input, boolean sum) {
        for (var item : sum ? add : subtract) {
            input = perform(input, item, sum);
        }
        return input;
    }

    private byte[] perform(byte[] input, byte[] buffer, boolean sum) {
        var expanded = Hkdf.extractAndExpand(buffer, salt, EXPAND_SIZE);
        var eRead = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        var tRead = ByteBuffer.wrap(expanded).order(ByteOrder.LITTLE_ENDIAN);
        var write = ByteBuffer.allocate(input.length).order(ByteOrder.LITTLE_ENDIAN);
        for (var index = 0; index < input.length; index += 2) {
            var first = Short.toUnsignedInt(eRead.getShort(index));
            var second = Short.toUnsignedInt(tRead.getShort(index));
            write.putShort(index, (short) (sum ? first + second : first - second));
        }
        var result = new byte[input.length];
        write.get(result);
        return result;
    }

    public record Result(byte[] hash, Map<Integer, byte[]> indexValueMap) {

    }
}
