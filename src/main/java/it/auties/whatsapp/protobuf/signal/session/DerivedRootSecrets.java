package it.auties.whatsapp.protobuf.signal.session;

import org.whispersystems.libsignal.util.ByteUtil;

import java.util.Arrays;

public record DerivedRootSecrets(byte[] rootKey, byte[] chainKey) {
    public static final int SIZE = 64;

    public DerivedRootSecrets(byte[] okm) {
        this(Arrays.copyOfRange(okm, 0, 32), Arrays.copyOfRange(okm, 32, 64));
    }
}
