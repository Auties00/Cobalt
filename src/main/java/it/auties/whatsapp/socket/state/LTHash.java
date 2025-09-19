package it.auties.whatsapp.socket.state;

import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.sync.RecordSync;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.*;

// TODO: Rewrite me
public final class LTHash {
    private static final byte[] SALT = "WhatsApp Patch Integrity".getBytes();

    private final byte[] hash;
    private final Map<Integer, byte[]> indexValueMap;
    private final List<byte[]> addList;
    private final List<byte[]> subList;

    public LTHash(CompanionHashState state) {
        this.hash = state.hash();
        this.indexValueMap = new HashMap<>(state.indexValueMap());
        this.addList = new ArrayList<>();
        this.subList = new ArrayList<>();
    }

    public void mix(byte[] indexMac, byte[] valueMac, RecordSync.Operation op) {
        var key = Arrays.hashCode(indexMac);
        var prev = indexValueMap.get(key);
        if (op == RecordSync.Operation.REMOVE) {
            if (prev != null) {
                indexValueMap.remove(key);
                subList.add(prev);
            }
        } else {
            addList.add(valueMac);
            indexValueMap.put(key, valueMac);
            if (prev != null) {
                subList.add(prev);
            }
        }
    }

    public Result finish() {
        var len = hash.length;
        if ((len & 1) != 0) {
            throw new IllegalStateException("Hash length must be even");
        }

        var shortCount = len / 2;
        var subSum = new int[shortCount];
        var addSum = new int[shortCount];

        for (var buf : subList) {
            accumulate(buf, subSum, shortCount);
        }

        for (var buf : addList) {
            accumulate(buf, addSum, shortCount);
        }

        var out = new byte[len];
        for (int i = 0, off = 0; i < shortCount; i++, off += 2) {
            var newVal = toUInt16(hash, off) - subSum[i] + addSum[i];
            out[off]     = (byte)(newVal & 0xFF);
            out[off + 1] = (byte)((newVal >>> 8) & 0xFF);
        }

        return new Result(out, indexValueMap);
    }

    private void accumulate(byte[] key, int[] sums, int shortCount) {
        try {
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(key, "AES"))
                    .thenExpand(SALT, hash.length);
            var exp = hkdf.deriveKey("AES", params)
                    .getEncoded();
            for (int i = 0, off = 0; i < shortCount; i++, off += 2) {
                sums[i] += toUInt16(exp, off);
            }
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot compute LTHash", exception);
        }
    }

    private static int toUInt16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    public record Result(byte[] hash, Map<Integer, byte[]> indexValueMap) {

    }
}