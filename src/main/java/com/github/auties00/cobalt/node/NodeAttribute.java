package com.github.auties00.cobalt.node;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.model.ProtobufString;

import java.util.*;

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
     *
     * @return a non-null string representation of this attribute value
     */
    String toValueString();

    /**
     * Converts this attribute value to its byte array representation.
     *
     * @return a non-null byte array representing this attribute value
     */
    byte[] toValueBytes();

    /**
     * Converts this attribute value to its jid representation.
     *
     * @return a non-null jid representation of this attribute value
     */
    Optional<Jid> toValueJid();

    OptionalLong toValueLong();

    OptionalDouble toValueDouble();

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
        public TextAttribute {
            Objects.requireNonNull(value, "value cannot be null");
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public String toValueString() {
            return value;
        }

        /**
         * Converts the text value to its UTF-8 byte representation.
         *
         * @return a non-null byte array containing the UTF-8 encoded text
         */
        @Override
        public byte[] toValueBytes() {
            return value.getBytes();
        }

        /**
         * Converts the text value to a {@link Jid}
         *
         * @return a non-null {@link Jid}
         * @throws MalformedJidException if the text value is not a jid
         */
        @Override
        public Optional<Jid> toValueJid() {
            try {
                var result = Jid.of(value);
                return Optional.of(result);
            }catch (MalformedJidException exception) {
                return Optional.empty();
            }
        }

        @Override
        public OptionalLong toValueLong() {
            try {
                var result = Long.parseLong(value);
                return OptionalLong.of(result);
            }catch (NumberFormatException exception) {
                return OptionalLong.empty();
            }
        }

        @Override
        public OptionalDouble toValueDouble() {
            try {
                var result = Double.parseDouble(value);
                return OptionalDouble.of(result);
            }catch (NumberFormatException exception) {
                return OptionalDouble.empty();
            }
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
        public JidAttribute {
            Objects.requireNonNull(value, "value cannot be null");
        }

        @Override
        public String toString() {
            return value.toString();
        }

        /**
         * Converts the Jid to its string representation.
         *
         * @return a non-null string representation of the Jid
         */
        @Override
        public String toValueString() {
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
        public byte[] toValueBytes() {
            return value.toString().getBytes();
        }

        /**
         * Returns the value that this attribute represents.
         *
         * @return a non-null {@link Jid}
         */
        @Override
        public Optional<Jid> toValueJid() {
            return Optional.of(value);
        }

        @Override
        public OptionalLong toValueLong() {
            return OptionalLong.empty();
        }

        @Override
        public OptionalDouble toValueDouble() {
            return OptionalDouble.empty();
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
        public BytesAttribute {
            Objects.requireNonNull(value, "value cannot be null");
        }

        @Override
        public String toString() {
            return Arrays.toString(value);
        }

        /**
         * Converts the binary data to a string representation.
         *
         * @return a non-null string decoded from the binary data
         */
        @Override
        public String toValueString() {
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
        public byte[] toValueBytes() {
            return value;
        }

        /**
         * Converts the binary data to a jid representation.
         *
         * @return a non-null {@link Jid}
         */
        @Override
        public Optional<Jid> toValueJid() {
            try {
                var result = Jid.of(ProtobufString.lazy(value));
                return Optional.of(result);
            }catch (MalformedJidException exception) {
                return Optional.empty();
            }
        }

        @Override
        public OptionalLong toValueLong() {
            try {
                var result = Long.parseLong(toValueString());
                return OptionalLong.of(result);
            }catch (NumberFormatException exception) {
                return OptionalLong.empty();
            }
        }

        @Override
        public OptionalDouble toValueDouble() {
            try {
                var result = Double.parseDouble(toValueString());
                return OptionalDouble.of(result);
            }catch (NumberFormatException exception) {
                return OptionalDouble.empty();
            }
        }
    }
}