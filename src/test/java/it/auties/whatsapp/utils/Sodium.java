package it.auties.whatsapp.utils;

import com.sun.jna.Library;

public interface Sodium extends Library {
    int crypto_box_seal(byte[] cipherText,
                        byte[] message, long messageLen,
                        byte[] recipientPublicKey);
}
