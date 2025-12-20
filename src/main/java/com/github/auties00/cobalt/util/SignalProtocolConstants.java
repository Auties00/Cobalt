package com.github.auties00.cobalt.util;

public final class SignalProtocolConstants {
    private SignalProtocolConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int BLOCK_SIZE = 16;

    public static final String SKMSG = "skmsg";
    public static final String PKMSG = "pkmsg";
    public static final String MSG = "msg";
    public static final String MSMSG = "msmsg";
}
