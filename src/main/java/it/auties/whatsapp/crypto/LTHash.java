package it.auties.whatsapp.crypto;

import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.sync.RecordSync;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LTHash {
    private static final int EXPAND_SIZE = 128;
    public static final String SALT = "WhatsApp Patch Integrity";

    private final byte[] salt;

    private final byte[] hash;

    private final Map<String, byte[]> indexValueMap;

    private final List<byte[]> add, subtract;

    public LTHash(CompanionHashState hash) {
        this.salt = SALT.getBytes(StandardCharsets.UTF_8);
        this.hash = hash.hash();
        this.indexValueMap = new HashMap<>(hash.indexValueMap());
        this.add = new ArrayList<>();
        this.subtract = new ArrayList<>();
    }

    public void mix(byte[] indexMac, byte[] valueMac, RecordSync.Operation operation) {
        var indexMacBase64 = Base64.getEncoder().encodeToString(indexMac);
        var prevOp = indexValueMap.get(indexMacBase64);
        if (operation == RecordSync.Operation.REMOVE) {
            if (prevOp == null) {
                return;
            }
            indexValueMap.remove(indexMacBase64, prevOp);
        } else {
            add.add(valueMac);
            indexValueMap.put(indexMacBase64, valueMac);
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

    public record Result(byte[] hash, Map<String, byte[]> indexValueMap) {
    }
}
