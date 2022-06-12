package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.sync.LTHashState;
import it.auties.whatsapp.model.sync.RecordSync;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LTHash {
    private static final int EXPAND_SIZE = 128;

    private final byte @NonNull [] salt;

    private final byte @NonNull [] hash;

    @NonNull
    private final Map<String, byte[]> indexValueMap;

    @NonNull
    private final List<byte[]> add, subtract;

    public LTHash(LTHashState hash) {
        this.salt = "WhatsApp Patch Integrity".getBytes(StandardCharsets.UTF_8);
        this.hash = hash.hash();
        this.indexValueMap = hash.indexValueMap();
        this.add = new ArrayList<>();
        this.subtract = new ArrayList<>();
    }

    public void mix(byte[] indexMac, byte[] valueMac, RecordSync.Operation operation) {
        var indexMacBase64 = Bytes.of(indexMac)
                .toBase64();
        var prevOp = indexValueMap.get(indexMacBase64);
        if (operation == RecordSync.Operation.REMOVE) {
            Validate.isTrue(prevOp != null, "No previous operation");
            indexValueMap.remove(indexMacBase64);
            subtract.add(prevOp);
            return;
        }

        add.add(valueMac);
        indexValueMap.put(indexMacBase64, valueMac);
        if (prevOp == null) {
            return;
        }

        subtract.add(prevOp);
    }

    public Result finish() {
        var subtracted = perform(hash, false);
        var added = perform(subtracted, true);
        return new Result(added, indexValueMap);
    }

    private byte[] perform(byte[] input, boolean sum) {
        for (var item : sum ?
                add :
                subtract) {
            input = perform(input, item, sum);
        }

        return input;
    }

    private byte[] perform(byte[] input, byte[] buffer, boolean sum) {
        var expanded = Hkdf.extractAndExpand(buffer, salt, EXPAND_SIZE);
        var eRead = ByteBuffer.wrap(input)
                .order(ByteOrder.LITTLE_ENDIAN);
        var tRead = ByteBuffer.wrap(expanded)
                .order(ByteOrder.LITTLE_ENDIAN);
        var write = ByteBuffer.allocate(input.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (var index = 0; index < input.length; index += 2) {
            var first = Short.toUnsignedInt(eRead.getShort(index));
            var second = Short.toUnsignedInt(tRead.getShort(index));
            write.putShort(index, (short) (sum ?
                    first + second :
                    first - second));
        }

        var result = new byte[input.length];
        write.get(result);
        return result;
    }

    public record Result(byte[] hash, Map<String, byte[]> indexValueMap) {

    }
}
