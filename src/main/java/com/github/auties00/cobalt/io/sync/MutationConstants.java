package com.github.auties00.cobalt.io.sync;

final class MutationConstants {
    static final int IV_LENGTH = 16;
    static final int MAC_LENGTH = 32;
    static final int MAX_PADDING_LENGTH = 64;
    static final byte[] VERSION = {0x00, 0x00, 0x00, 0x02};

    private MutationConstants() {
        throw new UnsupportedOperationException("MutationConstants is a utility class and cannot be instantiated");
    }
}
