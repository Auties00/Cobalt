package it.auties.whatsapp.io.node;

import it.auties.whatsapp.model.jid.Jid;

import java.nio.charset.StandardCharsets;

/**
 * A sealed interface representing attribute values within WhatsApp protocol nodes.
 * <p>
 * In the WhatsApp protocol, nodes are data structures that contain descriptive attributes,
 * similar to XML elements with their attributes. This interface defines the contract for
 * all possible attribute value types that can be attached to a {@link Node}.
 * <p>
 * Each implementation of this interface provides specific handling for different data types,
 * ensuring proper serialization to both string and byte representations for protocol transmission.
 * <p>
 * The interface is sealed to ensure type safety and exhaustive pattern matching, limiting
 * implementations to the three permitted subtypes defined within this file:
 * <ul>
 *   <li>{@link TextAttribute} - for plain text values</li>
 *   <li>{@link JidAttribute} - for WhatsApp identifier values</li>
 *   <li>{@link BytesAttribute} - for raw binary data</li>
 * </ul>
 *
 * @see Node
 * @see Jid
 */
public sealed interface NodeAttribute {
    /**
     * Converts this attribute value to its string representation.
     * <p>
     * The string representation is used for human-readable output and logging purposes.
     * The exact format depends on the implementing type.
     *
     * @return a non-null string representation of this attribute value
     */
    String toString();

    /**
     * Converts this attribute value to its byte array representation.
     * <p>
     * The byte representation is used for serialization when encoding nodes for
     * transmission over the WhatsApp protocol. The bytes are typically encoded
     * using UTF-8 charset.
     *
     * @return a non-null byte array representing this attribute value
     */
    byte[] toBytes();

    /**
     * A record representing a text-based attribute value.
     * <p>
     * This is the most common attribute type, used for simple string values
     * such as identifiers, type names, and other textual metadata within nodes.
     * <p>
     * The text is stored as-is and encoded to bytes using UTF-8 charset when needed.
     *
     * @param value the text value of this attribute, must not be null
     */
    record TextAttribute(String value) implements NodeAttribute {
        /**
         * Converts the text value to its UTF-8 byte representation.
         *
         * @return a non-null byte array containing the UTF-8 encoded text
         */
        @Override
        public byte[] toBytes() {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * A record representing a WhatsApp identifier (Jid) attribute value.
     * <p>
     * This attribute type is used when a node needs to reference a WhatsApp user,
     * group, or other entity identified by a {@link Jid}. The Jid is serialized
     * to its string representation for both display and transmission purposes.
     *
     * @param value the Jid value of this attribute, must not be null
     * @see Jid
     */
    record JidAttribute(Jid value) implements NodeAttribute {
        /**
         * Converts the Jid to its string representation.
         * <p>
         * The string format typically includes the user identifier and server
         * components of the Jid, such as "1234567890@s.whatsapp.net".
         *
         * @return a non-null string representation of the Jid
         */
        @Override
        public String toString() {
            return value.toString();
        }

        /**
         * Converts the Jid to its UTF-8 byte representation.
         * <p>
         * The Jid is first converted to its string form, then encoded to bytes
         * using UTF-8 charset for protocol transmission.
         *
         * @return a non-null byte array containing the UTF-8 encoded Jid string
         */
        @Override
        public byte[] toBytes() {
            return value.toString()
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * A record representing a raw binary data attribute value.
     * <p>
     * This attribute type is used when the attribute value consists of raw bytes
     * that should not be interpreted as text, or when the data is already in
     * its binary form (such as cryptographic hashes, tokens, or encoded data).
     * <p>
     * When converted to a string, the bytes are decoded using the platform's
     * default charset, which may not always produce meaningful text.
     *
     * @param value the binary value of this attribute, must not be null
     */
    record BytesAttribute(byte[] value) implements NodeAttribute {
        /**
         * Converts the binary data to a string representation.
         * <p>
         * The bytes are decoded using the platform's default charset. This may
         * produce unexpected results if the bytes do not represent valid text
         * in that charset.
         *
         * @return a non-null string decoded from the binary data
         */
        @Override
        public String toString() {
            return new String(value);
        }

        /**
         * Returns the binary data as-is.
         * <p>
         * Since this attribute already contains raw bytes, no conversion is needed.
         *
         * @return the original non-null byte array
         */
        @Override
        public byte[] toBytes() {
            return value;
        }
    }
}