package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Bytes;

import java.util.Arrays;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair {
    static byte[] toSignalKey(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 33 -> key;
            case 32 -> {
                var result = new byte[33];
                System.arraycopy(key, 0, result, 1, key.length);
                result[0] = 5;
                yield result;
            }
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    static byte[] toCurveKey(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 32 -> key;
            case 33 -> Arrays.copyOfRange(key, 1, key.length);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    byte[] privateKey();

    Node toNode();

    SignalKeyPair toGenericKeyPair();

    default byte[] signalPublicKey() {
        return toSignalKey(publicKey());
    }

    byte[] publicKey();

    default byte[] encodedId() {
        return Bytes.intToBytes(id(), 3);
    }

    default int id() {
        throw new UnsupportedOperationException(getClass().getName() + " doesn't provide an id");
    }
}
