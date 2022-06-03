package it.auties.whatsapp.util;

public interface SignalSpecification {
    int CURRENT_VERSION = 3;
    int IV_LENGTH = 16;
    int KEY_LENGTH = 32;
    int MAC_LENGTH = 8;
    int SIGNATURE_LENGTH = 64;
    int KEY_TYPE = 5;
    byte[] KEY_BUNDLE_TYPE = new byte[]{5};
}
