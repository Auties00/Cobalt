package com.github.auties00.cobalt.exception;

import java.util.HexFormat;
import java.util.Objects;

public final class WebAppStateSyncMissingKeyException
        extends WebAppStateSyncGenericRetryableException {
    private final byte[] keyId;
    public WebAppStateSyncMissingKeyException(byte[] keyId) {
        Objects.requireNonNull(keyId, "keyId cannot be null");
        super("Missing key with id " + HexFormat.of().formatHex(keyId));
        this.keyId = keyId;
    }

    public byte[] keyId() {
        return keyId;
    }
}
