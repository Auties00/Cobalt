package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.sync.MutationSync;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.Validate;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b512;

import java.util.*;
import java.util.function.BiFunction;

@Accessors(fluent = true)
public class LTHash {
    private static final int INT_SIZE_IN_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int CHECKSUM_SIZE = 2048;

    private final byte @NonNull [] hash;

    @NonNull
    @Getter
    private final Map<String, byte[]> indexValueMap;

    @NonNull
    private final Blake2b512 digest;

    private final List<byte[]> add, subtract;

    private byte[] checksum;

    public LTHash(byte[] hash){
        this.hash = hash;
        this.indexValueMap = new HashMap<>();
        this.digest = new Blake2b512();
        this.add = new ArrayList<>();
        this.subtract = new ArrayList<>();
        reset();
    }

    public void mix(byte[] indexMac, byte[] valueMac, MutationSync.Operation operation) {
        var indexMacBase64 = BinaryArray.of(indexMac).toBase64();
        var prevOp = indexValueMap.get(indexMacBase64);
        if (operation == MutationSync.Operation.REMOVE) {
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

    public byte[] finish() {
        subtract.add(0, hash);
        executeOperation((first, second) -> first - second, toArray(subtract));
        executeOperation(Integer::sum, toArray(add));
        return Arrays.copyOf(checksum, checksum.length);
    }

    public void reset() {
        this.checksum = BinaryArray.allocate(CHECKSUM_SIZE)
                .data();
    }

    private void executeOperation(BiFunction<Integer, Integer, Integer> function, byte[]... inputs) {
        Arrays.stream(inputs)
                .forEach(input -> executeOperation(function, input));
    }

    private void executeOperation(BiFunction<Integer, Integer, Integer> function, byte[] input) {
        var hash = hash(input);
        var result = Buffers.newBuffer();
        var checksumWrap = Buffers.newBuffer(checksum);
        var newHashWrap = Buffers.newBuffer(hash);
        for(var index = 0; index < hash.length; index += INT_SIZE_IN_BYTES) {
            var operation = function.apply(checksumWrap.readInt(), newHashWrap.readInt());
            result.writeInt(operation);
        }

        this.checksum = Buffers.readBytes(result);
    }

    private byte[] hash(byte[] input) {
        digest.update(input);
        return digest.digest();
    }

    private byte[][] toArray(List<byte[]> input) {
        var array = new byte[input.size()][];
        for(var index = 0; index < input.size(); index++){
            array[index] = input.get(index);
        }

        return array;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LTHash that
                && Arrays.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(checksum);
    }
}
