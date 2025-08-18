package it.auties.whatsapp.util;

import java.util.Arrays;

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
    public static final String MSMG = "msmsg";

    private SignalConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static byte[] createSignalKey(byte[] key) {
        if (key == null) {
            return null;
        }

        return switch (key.length) {
            case 33 -> key;
            case 32 -> {
                var result = new byte[KEY_LENGTH + 1];
                result[0] = KEY_TYPE;
                System.arraycopy(key, 0, result, 1, key.length);
                yield result;
            }
            default -> throw new IllegalArgumentException("Invalid key size");
        };
    }

    public static byte[] createCurveKey(byte[] key) {
        if (key == null) {
            return null;
        }

        return switch (key.length) {
            case 32 -> key;
            case 33 -> Arrays.copyOfRange(key, 1, 33);
            default -> throw new IllegalArgumentException("Invalid key size");
        };
    }
}
