package com.github.auties00.cobalt.message.signal;

final class SignalMessageConstants {
    private SignalMessageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static final int BLOCK_SIZE = 16;

    static final String SKMSG = "skmsg";
    static final String PKMSG = "pkmsg";
    static final String MSG = "msg";
    static final String MSMSG = "msmsg";
}
