package it.auties.whatsapp.util;

public interface SignalSpec {
    /**
     * The current version
     */
    int CURRENT_VERSION = 3;

    /**
     * Iv length
     */
    int IV_LENGTH = 16;

    /**
     * Curve25519 Key Length
     */
    int KEY_LENGTH = 32;

    /**
     * Mac length
     */
    int MAC_LENGTH = 8;

    /**
     * Signature length
     */
    int SIGNATURE_LENGTH = 64;
}
