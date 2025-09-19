package it.auties.whatsapp.model.signal.group.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

@ProtobufMessage
public final class SignalSenderChainKey {
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final int iteration;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private final byte[] seed;

    SignalSenderChainKey(int iteration, byte[] seed) {
        this.iteration = iteration;
        this.seed = seed;
    }

    public int iteration() {
        return iteration;
    }

    public byte[] seed() {
        return seed;
    }

    public SignalSenderMessageKey toSenderMessageKey() {
        return new SignalSenderMessageKey(iteration, getDerivative(MESSAGE_KEY_SEED, seed));
    }

    public SignalSenderChainKey next() {
        return new SignalSenderChainKey(iteration + 1, getDerivative(CHAIN_KEY_SEED, seed));
    }

    private byte[] getDerivative(byte[] seed, byte[] key) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            var keySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(seed);
        } catch (GeneralSecurityException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || obj instanceof SignalSenderChainKey that
                && this.iteration == that.iteration
                && Arrays.equals(this.seed, that.seed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iteration);
    }

    @Override
    public String toString() {
        return "SenderChainKey[" +
                "iteration=" + iteration + ", " +
                "seed=" + Arrays.toString(seed) + ']';
    }
}