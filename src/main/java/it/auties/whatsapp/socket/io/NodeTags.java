package it.auties.whatsapp.socket.io;

public final class NodeTags {
    private NodeTags() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final byte LIST_EMPTY = 0;
    public static final byte DICTIONARY_0 = (byte) 236;
    public static final byte DICTIONARY_1 = (byte) 237;
    public static final byte DICTIONARY_2 = (byte) 238;
    public static final byte DICTIONARY_3 = (byte) 239;
    public static final byte AD_JID = (byte) 247;
    public static final byte LIST_8 = (byte) 248;
    public static final byte LIST_16 = (byte) 249;
    public static final byte JID_PAIR = (byte) 250;
    public static final byte HEX_8 = (byte) 251;
    public static final byte BINARY_8 = (byte) 252;
    public static final byte BINARY_20 = (byte) 253;
    public static final byte BINARY_32 = (byte) 254;
    public static final byte NIBBLE_8 = (byte) 255;
}
