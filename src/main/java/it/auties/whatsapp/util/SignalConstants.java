package it.auties.whatsapp.util;

public final class SignalConstants {
    public static final int CURRENT_VERSION = 3;
    public static final int IV_LENGTH = 16;
    public static final int KEY_LENGTH = 32;
    public static final int MAC_LENGTH = 8;
    public static final int SIGNATURE_LENGTH = 64;
    public static final int KEY_TYPE = 5;
    public static final byte[] KEY_BUNDLE_TYPE = new byte[]{5};
    public static final int MAX_MESSAGES = 2000;
    public static final String SKMSG = "skmsg";
    public static final String PKMSG = "pkmsg";
    public static final String MSG = "msg";
    public static final String UNAVAILABLE = "unavailable";
}
