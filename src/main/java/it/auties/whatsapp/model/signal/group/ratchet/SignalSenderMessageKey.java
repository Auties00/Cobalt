package it.auties.whatsapp.model.signal.group.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

@ProtobufMessage
public final class SignalSenderMessageKey {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final int iteration;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private final byte[] seed;

    private final IvParameterSpec iv;

    private final SecretKeySpec cipherKey;

    SignalSenderMessageKey(int iteration, byte[] seed) {
        try {
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(seed, "AES"))
                    .thenExpand("WhisperGroup".getBytes(StandardCharsets.UTF_8), 48);
            var chunks = hkdf.deriveData(params);
            this.iteration = iteration;
            this.seed = seed;
            this.iv = new IvParameterSpec(chunks, 0, 16);
            this.cipherKey = new SecretKeySpec(chunks, 16, 32, "AES");
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    public int iteration() {
        return iteration;
    }

    public byte[] seed() {
        return seed;
    }

    public IvParameterSpec iv() {
        return iv;
    }

    public SecretKeySpec cipherKey() {
        return cipherKey;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof SignalSenderMessageKey that
                && this.iteration == that.iteration &&
                Arrays.equals(this.seed, that.seed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iteration, Arrays.hashCode(seed));
    }

    @Override
    public String toString() {
        return "SenderMessageKey[" +
                "iteration=" + iteration + ", " +
                "seed=" + Arrays.toString(seed) + ']';
    }
}