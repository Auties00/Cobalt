package it.auties.whatsapp.protobuf.signal.session;

import static java.util.Arrays.copyOfRange;

public record DerivedRootSecrets(byte[] rootKey, byte[] chainKey) {
    public static final int SIZE = 64;
    public DerivedRootSecrets(byte[] raw) {
        this(copyOfRange(raw, 0, 32), copyOfRange(raw, 32, 64));
    }
}
