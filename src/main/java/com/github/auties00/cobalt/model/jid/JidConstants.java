package com.github.auties00.cobalt.model.jid;

final class JidConstants {
    private JidConstants() {
        throw new UnsupportedOperationException("JidConstants is a utility class and should not be initialized");
    }

    static final char PHONE_CHAR = '+';
    static final char SERVER_CHAR = '@';
    static final char DEVICE_CHAR = ':';
    static final char AGENT_CHAR = '_';
}
