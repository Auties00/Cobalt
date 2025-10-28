package com.github.auties00.cobalt.node;

/**
 * A utility class that defines binary tag constants used in WhatsApp's node serialization protocol.
 * <p>
 * These tags are used during the encoding and decoding of WhatsApp protocol messages to identify
 * the type and structure of data elements within binary node representations. Each tag corresponds
 * to a specific data type or structure format used in the WhatsApp binary protocol.
 * </p>
 * <p>
 * This class cannot be instantiated as it only contains static constant definitions.
 * </p>
 *
 * @see Node
 * @see NodeEncoder
 * @see NodeDecoder
 */
public final class NodeTags {
    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always, as this class should not be instantiated
     */
    private NodeTags() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Tag indicating an empty list structure with no elements.
     */
    public static final byte LIST_EMPTY = 0;

    /**
     * Tag for dictionary lookup in {@link NodeTokens#DICTIONARY_0_TOKENS}.
     * Used for compact string representation by referencing predefined tokens.
     */
    public static final byte DICTIONARY_0 = (byte) 236;

    /**
     * Tag for dictionary lookup in {@link NodeTokens#DICTIONARY_1_TOKENS}.
     * Used for compact string representation by referencing predefined tokens.
     */
    public static final byte DICTIONARY_1 = (byte) 237;

    /**
     * Tag for dictionary lookup in {@link NodeTokens#DICTIONARY_2_TOKENS}.
     * Used for compact string representation by referencing predefined tokens.
     */
    public static final byte DICTIONARY_2 = (byte) 238;

    /**
     * Tag for dictionary lookup in {@link NodeTokens#DICTIONARY_3_TOKENS}.
     * Used for compact string representation by referencing predefined tokens.
     */
    public static final byte DICTIONARY_3 = (byte) 239;

    /**
     * Tag indicating an advertisement JID.
     * Used for special WhatsApp advertisement-related identifiers.
     */
    public static final byte AD_JID = (byte) 247;

    /**
     * Tag indicating a list structure with up to 255 elements (8-bit length).
     * The next byte specifies the number of elements in the list.
     */
    public static final byte LIST_8 = (byte) 248;

    /**
     * Tag indicating a list structure with up to 65535 elements (16-bit length).
     * The next two bytes specify the number of elements in the list.
     */
    public static final byte LIST_16 = (byte) 249;

    /**
     * Tag indicating a JID pair structure.
     * Represents a WhatsApp JID consisting of user and server parts.
     */
    public static final byte JID_PAIR = (byte) 250;

    /**
     * Tag indicating an 8-bit hexadecimal encoded value.
     * The next byte specifies the length, followed by hexadecimal data.
     */
    public static final byte HEX_8 = (byte) 251;

    /**
     * Tag indicating binary data with 8-bit length.
     * The next 8 bits specify the length (0-255 bytes), followed by the binary data.
     */
    public static final byte BINARY_8 = (byte) 252;

    /**
     * Tag indicating binary data with 20-bit length.
     * The next 20 bits specify the length, followed by the binary data.
     */
    public static final byte BINARY_20 = (byte) 253;

    /**
     * Tag indicating binary data with 32-bit length.
     * The next 32 bits specify the length, followed by the binary data.
     */
    public static final byte BINARY_32 = (byte) 254;

    /**
     * Tag indicating an 8-bit nibble-packed value.
     * Used for compact encoding.
     */
    public static final byte NIBBLE_8 = (byte) 255;
}