package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.sync.LTHashState;
import it.auties.whatsapp.protobuf.sync.MutationSync;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.Validate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b512;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

public class LTHash {
    @NonNull
    private final LTHashImplementation implementation;

    @NonNull
    private final byte[] hash;

    @NonNull
    private final Map<String, byte[]> indexValueMap;

    @NonNull
    private final List<byte[]> add, subtract;

    public LTHash(LTHashState hash){
        this.implementation = new LTHashImplementation("WhatsApp Patch Integrity");
        this.hash = hash.hash();
        this.indexValueMap = hash.indexValueMap();
        this.add = new ArrayList<>();
        this.subtract = new ArrayList<>();
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

    public Result finish() {
        var result = implementation.subtractThenAdd(hash,
                add.toArray(byte[][]::new), subtract.toArray(byte[][]::new));
        return new Result(result, indexValueMap);
    }

    public record Result(byte[] hash, Map<String, byte[]> indexValueMap){

    }

    private record LTHashImplementation(@NonNull String salt) {
        public byte[] add(byte[] input, byte[][] buffers) {
            for(var item : buffers) {
                input = addSingle(input, item);
            }

            return input;
        }

        public byte[] subtract(byte[] input, byte[][] buffers) {
            for(var item : buffers) {
                input = subtractSingle(input, item);
            }

            return input;
        }

        public byte[] subtractThenAdd(byte[] input, byte[][] addBuffers, byte[][] removeBuffers) {
            return add(subtract(input, removeBuffers), addBuffers);
        }

        public byte[] addSingle(byte[] input, byte[] addBuffer) {
            var expanded = Hkdf.expand(addBuffer, salt.getBytes(StandardCharsets.UTF_8), 128);
            return performPointWiseWithOverflow(input, expanded, Integer::sum);
        }

        public byte[] subtractSingle(byte[] input, byte[] removeBuffer) {
            var expanded = Hkdf.expand(removeBuffer, salt.getBytes(StandardCharsets.UTF_8), 128);
            return performPointWiseWithOverflow(input, expanded, (first, second) -> first - second);
        }

        public byte[] performPointWiseWithOverflow(byte[] input, byte[] buffer, BiFunction<Integer, Integer, Integer> function) {
            var eRead = Buffers.newBuffer(input);
            var tRead = Buffers.newBuffer(buffer);
            var write = Buffers.newBuffer();
            while (eRead.isReadable()){
                var first = eRead.readUnsignedShort();
                var second = tRead.readUnsignedShort();
                write.writeShort(function.apply(first, second));
            }

            return Buffers.readBytes(write);
        }
    }
}
