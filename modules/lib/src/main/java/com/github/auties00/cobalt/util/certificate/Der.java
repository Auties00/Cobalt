package com.github.auties00.cobalt.util.certificate;

import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A builder-style DER (Distinguished Encoding Rules) encoder that assembles an ASN.1 value as a tree of
 * {@link Node}s and serializes it into a single, exactly-sized buffer in one pass.
 *
 * <p>The factory methods ({@link #integer(BigInteger)}, {@link #oid(String)}, {@link #time(Instant)}, and the
 * rest) and the {@link #sequence()} builder produce {@link Node}s rather than byte arrays, so composing a nested
 * structure allocates only the lightweight stanza objects, never an intermediate encoding. The lengths are
 * computed bottom-up exactly once when {@link Node#encode()} sizes the output buffer, and the bytes are written
 * straight into that one buffer; no value is encoded into a temporary array and then copied into a larger one,
 * which is the wasteful pattern a per-TLV encoder forces on a deeply nested document like an X.509 certificate.
 *
 * <p>This is an encoder, not a parser, and covers only the pieces X.509 assembly needs that are not already
 * produced by a JDK type ({@link javax.security.auth.x500.X500Principal#getEncoded()} for the {@code Name},
 * {@link java.security.PublicKey#getEncoded()} for the {@code SubjectPublicKeyInfo}, and {@link java.security.Signature}
 * for the signature value, all spliced in with {@link #raw(byte[])}).
 *
 * @implNote This implementation only ever emits the definite short or long length form, never the indefinite
 *           form, which is what DER (as opposed to plain BER) requires. Correctness is established by feeding the
 *           assembled certificate through the JDK's own X.509 {@link java.security.cert.CertificateFactory},
 *           which rejects malformed DER.
 */
final class Der {
    /**
     * Holds the ASN.1 universal tag for {@code INTEGER}.
     */
    private static final int TAG_INTEGER = 0x02;

    /**
     * Holds the ASN.1 universal tag for {@code BIT STRING}.
     */
    private static final int TAG_BIT_STRING = 0x03;

    /**
     * Holds the ASN.1 universal tag for {@code NULL}.
     */
    private static final int TAG_NULL = 0x05;

    /**
     * Holds the ASN.1 universal tag for {@code OBJECT IDENTIFIER}.
     */
    private static final int TAG_OBJECT_IDENTIFIER = 0x06;

    /**
     * Holds the ASN.1 universal tag for a constructed {@code SEQUENCE}.
     */
    private static final int TAG_SEQUENCE = 0x30;

    /**
     * Holds the ASN.1 universal tag for {@code UTCTime}.
     */
    private static final int TAG_UTC_TIME = 0x17;

    /**
     * Holds the ASN.1 universal tag for {@code GeneralizedTime}.
     */
    private static final int TAG_GENERALIZED_TIME = 0x18;

    /**
     * Holds the first calendar year (inclusive) for which RFC 5280 mandates {@code GeneralizedTime} rather than
     * {@code UTCTime}.
     */
    private static final int GENERALIZED_TIME_YEAR = 2050;

    /**
     * Holds the earliest calendar year (inclusive) representable by the two-digit {@code UTCTime} window RFC 5280
     * pins (years {@code 50}-{@code 99} map to {@code 19xx}).
     */
    private static final int UTC_TIME_MIN_YEAR = 1950;

    /**
     * Holds the {@code UTCTime} formatter, {@code YYMMDDHHMMSSZ} in UTC.
     */
    private static final DateTimeFormatter UTC_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    /**
     * Holds the {@code GeneralizedTime} formatter, {@code YYYYMMDDHHMMSSZ} in UTC.
     */
    private static final DateTimeFormatter GENERALIZED_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    /**
     * Holds the shared empty value used for the {@code NULL} primitive.
     */
    private static final byte[] EMPTY = new byte[0];

    /**
     * Prevents instantiation of this static factory.
     */
    private Der() {
        throw new AssertionError("Der is a utility holder and cannot be instantiated");
    }

    /**
     * Builds an {@code INTEGER} stanza from a {@link BigInteger}.
     *
     * <p>{@link BigInteger#toByteArray()} already yields the minimal big-endian two's-complement encoding,
     * including the leading {@code 0x00} a positive value with its high bit set needs to stay positive, so it is
     * the DER {@code INTEGER} content verbatim.
     *
     * @param value the integer value
     * @return the {@code INTEGER} stanza
     */
    static Node integer(BigInteger value) {
        return new Primitive(TAG_INTEGER, value.toByteArray());
    }

    /**
     * Builds an {@code OBJECT IDENTIFIER} stanza from its dotted-decimal form.
     *
     * @param dotted the dotted-decimal OID, for example {@code "1.2.840.10045.4.3.2"}
     * @return the {@code OBJECT IDENTIFIER} stanza
     * @throws IllegalArgumentException if {@code dotted} has fewer than two arcs or a non-numeric arc
     */
    static Node oid(String dotted) {
        return new Primitive(TAG_OBJECT_IDENTIFIER, encodeOid(dotted));
    }

    /**
     * Builds a {@code BIT STRING} stanza whose bits are exactly the given octets with no unused trailing bits.
     *
     * @param bytes the bit-string octets
     * @return the {@code BIT STRING} stanza
     */
    static Node bitString(byte[] bytes) {
        return new BitString(bytes);
    }

    /**
     * Builds the ASN.1 {@code NULL} stanza.
     *
     * @return the {@code NULL} stanza
     */
    static Node nullValue() {
        return new Primitive(TAG_NULL, EMPTY);
    }

    /**
     * Builds an X.509 {@code Time} stanza, choosing the representation RFC 5280 mandates for the year.
     *
     * <p>The instant is truncated to whole seconds and rendered in UTC. Years in {@code [1950, 2050)} use
     * {@code UTCTime} ({@code YYMMDDHHMMSSZ}); all others use {@code GeneralizedTime} ({@code YYYYMMDDHHMMSSZ}).
     *
     * @param instant the point in time
     * @return the {@code UTCTime} or {@code GeneralizedTime} stanza
     */
    static Node time(Instant instant) {
        var truncated = instant.truncatedTo(ChronoUnit.SECONDS);
        var year = truncated.atZone(ZoneOffset.UTC).getYear();
        if (year >= UTC_TIME_MIN_YEAR && year < GENERALIZED_TIME_YEAR) {
            return new Primitive(TAG_UTC_TIME, UTC_TIME_FORMAT.format(truncated).getBytes(StandardCharsets.US_ASCII));
        }
        return new Primitive(TAG_GENERALIZED_TIME, GENERALIZED_TIME_FORMAT.format(truncated).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Wraps an already-DER-encoded value as a stanza so it can be spliced into a larger structure without being
     * re-encoded.
     *
     * <p>Used for the {@code Name} and {@code SubjectPublicKeyInfo} blobs the JDK hands back pre-encoded, and for
     * a {@code TBSCertificate} that has been encoded once so it could be signed.
     *
     * @param der an already-DER-encoded value
     * @return a stanza that emits {@code der} verbatim
     */
    static Node raw(byte[] der) {
        return new Raw(der);
    }

    /**
     * Builds a context-specific {@code [tagNumber] EXPLICIT} stanza around another stanza.
     *
     * @param tagNumber the context-specific tag number
     * @param content   the stanza being tagged
     * @return the explicitly-tagged stanza
     */
    static Node explicit(int tagNumber, Node content) {
        return new Constructed(0xA0 | tagNumber, List.of(content));
    }

    /**
     * Begins building a constructed {@code SEQUENCE} stanza.
     *
     * @return a fresh sequence builder
     */
    static SequenceBuilder sequence() {
        return new SequenceBuilder();
    }

    /**
     * A stanza in the ASN.1 value tree that can report its own encoded length and write itself into a buffer.
     *
     * <p>Sizing and writing are split so the whole tree can be measured once to allocate one exactly-sized
     * buffer, then written into it in a single pass.
     */
    sealed interface Node permits Primitive, BitString, Constructed, Raw {
        /**
         * Returns the number of bytes this stanza occupies once encoded.
         *
         * @return the encoded length
         */
        int encodedLength();

        /**
         * Writes this stanza's encoding into {@code out} starting at {@code offset}.
         *
         * @param out    the destination buffer, sized to hold the whole document
         * @param offset the index at which to start writing
         * @return the index immediately past the bytes written
         */
        int writeTo(byte[] out, int offset);

        /**
         * Encodes this stanza into a fresh, exactly-sized byte array.
         *
         * @return the DER encoding of this stanza
         */
        default byte[] encode() {
            var out = new byte[encodedLength()];
            writeTo(out, 0);
            return out;
        }
    }

    /**
     * A primitive stanza: a tag wrapping a fixed value body.
     *
     * @param tag   the ASN.1 tag octet
     * @param value the value body
     */
    record Primitive(int tag, byte[] value) implements Node {
        @Override
        public int encodedLength() {
            return 1 + lengthOfLength(value.length) + value.length;
        }

        @Override
        public int writeTo(byte[] out, int offset) {
            out[offset++] = (byte) tag;
            offset = writeLength(out, offset, value.length);
            return DataUtils.append(out, offset, value);
        }
    }

    /**
     * A stanza that emits an already-encoded value verbatim.
     *
     * @param der the complete encoded value
     */
    record Raw(byte[] der) implements Node {
        @Override
        public int encodedLength() {
            return der.length;
        }

        @Override
        public int writeTo(byte[] out, int offset) {
            return DataUtils.append(out, offset, der);
        }
    }

    /**
     * A {@code BIT STRING} stanza whose bits are exactly the held octets with no unused trailing bits.
     *
     * <p>The mandatory {@code 0x00} unused-bit-count octet is written straight into the output buffer ahead of
     * the octets, so the held array is never copied into a larger one just to prepend that byte.
     *
     * @param bytes the bit-string octets
     */
    record BitString(byte[] bytes) implements Node {
        @Override
        public int encodedLength() {
            var content = bytes.length + 1;
            return 1 + lengthOfLength(content) + content;
        }

        @Override
        public int writeTo(byte[] out, int offset) {
            out[offset++] = (byte) TAG_BIT_STRING;
            offset = writeLength(out, offset, bytes.length + 1);
            out[offset++] = 0x00;
            return DataUtils.append(out, offset, bytes);
        }
    }

    /**
     * A constructed stanza: a tag wrapping an ordered list of child nodes.
     */
    static final class Constructed implements Node {
        /**
         * Holds the ASN.1 tag octet of this constructed value.
         */
        private final int tag;

        /**
         * Holds the ordered child nodes this value wraps.
         */
        private final List<Node> children;

        /**
         * Constructs a constructed stanza over a tag and its children.
         *
         * @param tag      the ASN.1 tag octet
         * @param children the ordered children
         */
        Constructed(int tag, List<Node> children) {
            this.tag = tag;
            this.children = children;
        }

        /**
         * Returns the summed encoded length of the children, the content this value wraps.
         *
         * @return the content length
         */
        private int contentLength() {
            var total = 0;
            for (var child : children) {
                total += child.encodedLength();
            }
            return total;
        }

        @Override
        public int encodedLength() {
            var content = contentLength();
            return 1 + lengthOfLength(content) + content;
        }

        @Override
        public int writeTo(byte[] out, int offset) {
            out[offset++] = (byte) tag;
            offset = writeLength(out, offset, contentLength());
            for (var child : children) {
                offset = child.writeTo(out, offset);
            }
            return offset;
        }
    }

    /**
     * A fluent builder that accumulates the children of a {@code SEQUENCE} and produces the stanza.
     */
    static final class SequenceBuilder {
        /**
         * Holds the children added so far, in order.
         */
        private final List<Node> children = new ArrayList<>();

        /**
         * Restricts construction to {@link Der#sequence()}.
         */
        private SequenceBuilder() {
        }

        /**
         * Appends a child stanza to the sequence.
         *
         * @param node the child to append
         * @return this builder
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        SequenceBuilder add(Node node) {
            children.add(Objects.requireNonNull(node, "stanza cannot be null"));
            return this;
        }

        /**
         * Produces the immutable {@code SEQUENCE} stanza over the accumulated children.
         *
         * @return the sequence stanza
         */
        Node build() {
            return new Constructed(TAG_SEQUENCE, List.copyOf(children));
        }

        /**
         * Builds the sequence and encodes it into a fresh, exactly-sized byte array.
         *
         * @return the DER encoding of the sequence
         */
        byte[] encode() {
            return build().encode();
        }
    }

    /**
     * Returns the number of bytes the DER definite length form needs for a content length.
     *
     * @param length the non-negative content length
     * @return the number of length octets
     */
    private static int lengthOfLength(int length) {
        if (length < 0x80) {
            return 1;
        }
        return 1 + (Integer.SIZE - Integer.numberOfLeadingZeros(length) + 7) / 8;
    }

    /**
     * Writes a DER definite length into {@code out} at {@code offset}.
     *
     * @param out    the destination buffer
     * @param offset the index at which to start writing
     * @param length the non-negative content length
     * @return the index immediately past the length octets
     */
    private static int writeLength(byte[] out, int offset, int length) {
        if (length < 0x80) {
            out[offset++] = (byte) length;
            return offset;
        }
        var sizeBytes = (Integer.SIZE - Integer.numberOfLeadingZeros(length) + 7) / 8;
        out[offset++] = (byte) (0x80 | sizeBytes);
        for (var i = sizeBytes - 1; i >= 0; i--) {
            out[offset++] = (byte) (length >>> (8 * i));
        }
        return offset;
    }

    /**
     * Encodes the body of an {@code OBJECT IDENTIFIER} (without the tag and length) from its dotted-decimal form.
     *
     * <p>The first two arcs are folded into one value {@code 40 * arc0 + arc1}; each remaining arc is encoded
     * base-128, most significant group first, with the high bit set on every octet except the last of the arc.
     * Arcs are located with {@link String#indexOf(int, int)} and parsed in place with
     * {@link Long#parseLong(CharSequence, int, int, int)} over the dotted string, so neither an arc array nor a
     * substring per arc is allocated; the only allocation is the returned body, which is sized in a first scan
     * and filled in a second.
     *
     * @param dotted the dotted-decimal OID
     * @return the encoded OID body
     * @throws IllegalArgumentException if {@code dotted} has fewer than two arcs or a non-numeric arc
     */
    private static byte[] encodeOid(String dotted) {
        var firstDot = dotted.indexOf('.');
        if (firstDot < 0) {
            throw new IllegalArgumentException("OID must have at least two arcs: " + dotted);
        }
        var secondDot = dotted.indexOf('.', firstDot + 1);
        var firstArcEnd = secondDot < 0 ? dotted.length() : secondDot;
        var combined = 40L * Long.parseLong(dotted, 0, firstDot, 10)
                + Long.parseLong(dotted, firstDot + 1, firstArcEnd, 10);

        var length = base128Length(combined);
        for (var start = firstArcEnd; start < dotted.length(); ) {
            var end = dotted.indexOf('.', start + 1);
            if (end < 0) {
                end = dotted.length();
            }
            length += base128Length(Long.parseLong(dotted, start + 1, end, 10));
            start = end;
        }

        var body = new byte[length];
        var offset = writeBase128(body, 0, combined);
        for (var start = firstArcEnd; start < dotted.length(); ) {
            var end = dotted.indexOf('.', start + 1);
            if (end < 0) {
                end = dotted.length();
            }
            offset = writeBase128(body, offset, Long.parseLong(dotted, start + 1, end, 10));
            start = end;
        }
        return body;
    }

    /**
     * Returns the number of base-128 octets the given non-negative arc value occupies.
     *
     * @param value the arc value
     * @return the octet count, at least one
     */
    private static int base128Length(long value) {
        var length = 1;
        for (var remaining = value >>> 7; remaining > 0; remaining >>>= 7) {
            length++;
        }
        return length;
    }

    /**
     * Writes one OID arc as base-128 octets into {@code out}, the high bit set on all but the final octet.
     *
     * @param out    the destination buffer
     * @param offset the index at which to start writing
     * @param value  the non-negative arc value
     * @return the index immediately past the octets written
     */
    private static int writeBase128(byte[] out, int offset, long value) {
        for (var i = base128Length(value) - 1; i >= 0; i--) {
            var octet = (int) ((value >>> (7 * i)) & 0x7F);
            out[offset++] = (byte) (i == 0 ? octet : octet | 0x80);
        }
        return offset;
    }
}
