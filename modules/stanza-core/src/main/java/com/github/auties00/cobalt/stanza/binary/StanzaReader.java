package com.github.auties00.cobalt.stanza.binary;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaAttribute;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.github.auties00.cobalt.stanza.binary.StanzaTags.*;
import static com.github.auties00.cobalt.stanza.binary.StanzaTokens.*;

/**
 * Parses {@link Stanza} trees from WhatsApp's binary stanza format.
 *
 * <p>Inbound stanzas arrive as raw or DEFLATE-compressed byte sequences
 * encoded by the WAWap protocol. This class consumes the leading flags
 * byte to choose between an uncompressed and an inflating decoder, then
 * walks the size header, description, attribute pairs and typed content
 * slot (sized child list, JID variant, hex or nibble packed string,
 * dictionary token, single-byte token, or binary blob) to rebuild the
 * stanza tree.
 *
 * <p>The class is an abstract template: the tree-traversal logic lives
 * here once and subclasses provide only the four primitive read
 * operations ({@link #read()}, {@link #readShort()}, {@link #readInt()},
 * {@link #readBytes(int)}) plus {@link #hasData()} specialised for the
 * backing source. Four sources are supported through static factories,
 * each automatically picking the compressed or uncompressed variant
 * based on the leading flags byte:
 *
 * <ul>
 *   <li>{@link #fromBytes(byte[])} reads from a {@code byte[]} starting
 *       at offset zero
 *   <li>{@link #fromBuffer(ByteBuffer)} reads from a {@link ByteBuffer}
 *       starting at its current position, advancing it as bytes are
 *       consumed
 *   <li>{@link #fromSegment(MemorySegment, long)} reads from a
 *       {@link MemorySegment} starting at the supplied byte offset
 *   <li>{@link #fromStream(InputStream, int)} reads one stanza bounded
 *       to the next {@code length} bytes of an {@link InputStream}
 *       through a per-instance 8 KiB staging buffer (so wrapping the
 *       input in {@link java.io.BufferedInputStream} is unnecessary),
 *       leaving {@link #drain()} to advance a continuous
 *       multi-frame source onto the next frame boundary
 * </ul>
 *
 * <p>Decoders are {@link AutoCloseable}: a compressed decoder releases
 * its {@link Inflater} on close; an uncompressed decoder is a no-op. The
 * source is always caller-owned and left open.
 *
 * @see Stanza
 * @see StanzaAttribute
 * @see StanzaWriter
 * @see StanzaTokens
 * @see StanzaTags
 */
@WhatsAppWebModule(moduleName = "WAWap")
public abstract class StanzaReader implements AutoCloseable {

    /**
     * The logger for {@link StanzaReader}.
     */
    private static final System.Logger LOGGER = Log.get(StanzaReader.class);

    /**
     * Holds the decoder alphabet for nibble packed strings.
     *
     * <p>The four high entries are explicit replacement characters because the
     * wire format reserves those slots without assigning them a textual
     * meaning; any byte that would index into them is a protocol-level encoder
     * bug.
     */
    private static final char[] NIBBLE_ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�', '�', '�'};

    /**
     * Holds the decoder alphabet for hex packed strings (uppercase hex digits
     * only).
     */
    private static final char[] HEX_ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Holds the default capacity of the per-instance staging buffers used by
     * the stream-backed decoders.
     *
     * <p>The compressed stream variant uses two buffers of this size (one for
     * inflater input, one for inflated output) so its per-decoder cost is
     * 16 KiB.
     *
     * @implNote
     * 8 KiB matches the default that {@link java.io.BufferedInputStream}
     * picks: large enough to absorb most stanzas in one fill and small enough
     * that the per-decoder allocation cost is negligible.
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Holds the bit mask in the leading flags byte signalling that the stanza
     * payload is DEFLATE compressed.
     */
    private static final int COMPRESSION_FLAG = 2;

    /**
     * Constructs a new decoder for a package-internal subclass.
     *
     * <p>The hierarchy is closed within this package; the constructor exists
     * only to satisfy {@code javac} for the inner subclasses.
     */
    StanzaReader() {

    }

    /**
     * Returns a decoder appropriate for the leading flags byte of the supplied
     * {@code byte[]} source starting at offset zero.
     *
     * <p>Equivalent to {@code fromBytes(source, 0)} for callers that have the
     * entire encoded stanza in a fresh array.
     *
     * @param source the array of encoded stanza bytes, starting with the flags
     *               byte at index zero
     * @return an inflating decoder when the compression flag is set, a direct
     *         decoder otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "decodeStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static StanzaReader fromBytes(byte[] source) {
        return fromBytes(source, 0);
    }

    /**
     * Returns a decoder appropriate for the leading flags byte of the supplied
     * {@code byte[]} source starting at {@code offset}.
     *
     * <p>Used when the flags byte sits inside a larger framing buffer (for
     * example a Noise tunnel decryption output) and the caller does not want
     * to slice the array.
     *
     * @param source the array of encoded stanza bytes
     * @param offset the index of the flags byte within {@code source}
     * @return an inflating decoder when the compression flag is set, a direct
     *         decoder otherwise
     */
    public static StanzaReader fromBytes(byte[] source, int offset) {
        Objects.requireNonNull(source, "source");
        var flags = source[offset] & 0xFF;
        if ((flags & COMPRESSION_FLAG) != 0) {
            return new ByteArrayCompressed(source, offset + 1);
        }
        return new ByteArrayUncompressed(source, offset + 1);
    }

    /**
     * Returns a decoder appropriate for the leading flags byte of the supplied
     * {@link ByteBuffer} source.
     *
     * <p>The buffer's current position must be at the flags byte; the decoder
     * advances the position as it consumes the stanza, so a caller that wants
     * to peek without committing must {@link ByteBuffer#duplicate()} first.
     *
     * @param source the buffer of encoded stanza bytes, in read mode with the
     *               flags byte at its current position
     * @return an inflating decoder when the compression flag is set, a direct
     *         decoder otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "decodeStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static StanzaReader fromBuffer(ByteBuffer source) {
        Objects.requireNonNull(source, "source");
        var flags = source.get() & 0xFF;
        if ((flags & COMPRESSION_FLAG) != 0) {
            return new ByteBufferCompressed(source);
        }
        return new ByteBufferUncompressed(source);
    }

    /**
     * Returns a decoder appropriate for the leading flags byte of the supplied
     * {@link MemorySegment} source starting at {@code offset}.
     *
     * <p>Used by callers that own a foreign memory segment containing the
     * stanza (for example a frame from an off-heap socket buffer); avoids the
     * round-trip through a {@code byte[]}.
     *
     * @param source the segment of encoded stanza bytes
     * @param offset the byte offset of the flags byte within {@code source}
     * @return an inflating decoder when the compression flag is set, a direct
     *         decoder otherwise
     */
    public static StanzaReader fromSegment(MemorySegment source, long offset) {
        Objects.requireNonNull(source, "source");
        var flags = source.get(ValueLayout.JAVA_BYTE, offset) & 0xFF;
        if ((flags & COMPRESSION_FLAG) != 0) {
            return new MemorySegmentCompressed(source, offset + 1);
        }
        return new MemorySegmentUncompressed(source, offset + 1);
    }

    /**
     * Returns a decoder for a single stanza that occupies exactly {@code length}
     * plaintext bytes of the supplied caller-owned {@link InputStream}.
     *
     * <p>The decoder reads at most {@code length} bytes from {@code source}: once
     * those bytes are exhausted the read primitives report end-of-stream, so a
     * stanza can be streamed off one datagram frame without ever crossing into
     * the following frame even though {@code source} is a continuous multi-frame
     * stream. The leading flags byte (counted within {@code length}) is consumed
     * immediately to pick the uncompressed or compressed variant; the decoder
     * pulls bulk reads of up to 8 KiB from {@code source} so wrapping the input
     * in {@link java.io.BufferedInputStream} is unnecessary, it never materialises
     * the whole frame, and it does not close {@code source} on {@link #close()}.
     *
     * <p>A well-formed stanza consumes its frame exactly; when it does not (the
     * frame carries trailing bytes, or {@link #decode()} fails part way through),
     * {@link #drain()} drains whatever is left so {@code source} lands on
     * the next frame's length prefix.
     *
     * @param source the input stream of encoded stanza bytes, positioned at the
     *               flags byte of the frame
     * @param length the frame's plaintext byte count, including the flags byte
     * @return an inflating decoder when the compression flag is set, a direct
     *         decoder otherwise
     * @throws EOFException if the stream is already at end-of-stream so no flags
     *         byte can be read
     * @throws IOException if reading the flags byte otherwise fails
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public static StanzaReader fromStream(InputStream source, int length) throws IOException {
        Objects.requireNonNull(source, "source");
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive, got " + length);
        }
        var buffer = new byte[DEFAULT_BUFFER_SIZE];
        var prefilledBytes = source.read(buffer, 0, Math.min(DEFAULT_BUFFER_SIZE, length));
        if (prefilledBytes <= 0) {
            throw new EOFException("Unexpected end of stream while reading flags byte");
        }
        var frameRemaining = length - prefilledBytes;
        var flags = buffer[0] & 0xFF;
        if ((flags & COMPRESSION_FLAG) != 0) {
            return new StreamCompressed(source, buffer, prefilledBytes, frameRemaining);
        }
        return new StreamUncompressed(source, buffer, prefilledBytes, frameRemaining);
    }

    /**
     * Decodes the root {@link Stanza} of the stanza in the source.
     *
     * <p>Callers obtain a decoder from one of the {@code from...} factories,
     * call this method, then close the decoder.
     *
     * @return the decoded stanza
     * @throws IOException if the input is truncated, malformed, or fails to
     *         decompress
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "decodeStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public final Stanza decode() throws IOException {
        var stanza = readStanza();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "stanza decoded {0}", stanza);
        return stanza;
    }

    /**
     * Returns whether this decoder still has bytes available to be read.
     *
     * <p>Lets callers decode a contiguous stream of stanzas from the same
     * decoder without knowing the boundary in advance. Returns {@code true}
     * both for buffered-but-unread bytes and for source bytes the decoder has
     * not yet drained.
     *
     * @return {@code true} when more bytes remain to be read
     */
    public abstract boolean hasData();

    /**
     * Reads the next byte as an unsigned value in the {@code 0..255}
     * range.
     *
     * @implSpec
     * Subclasses must return the next byte zero-extended to an
     * {@code int} and advance the source position by one. An empty
     * source must throw {@link IOException}.
     *
     * @return the next byte in the {@code 0..255} range
     * @throws IOException if no more bytes are available
     */
    abstract int read() throws IOException;

    /**
     * Reads the next two bytes as an unsigned 16-bit big-endian
     * integer.
     *
     * @implSpec
     * Subclasses must return the next two bytes interpreted in
     * big-endian order zero-extended to an {@code int} and advance
     * the source position by two.
     *
     * @return the next 16-bit value in the {@code 0..65535} range
     * @throws IOException if fewer than two bytes are available
     */
    abstract int readShort() throws IOException;

    /**
     * Reads the next four bytes as a signed 32-bit big-endian
     * integer.
     *
     * @implSpec
     * Subclasses must return the next four bytes interpreted in
     * big-endian order and advance the source position by four.
     *
     * @return the next 32-bit signed value
     * @throws IOException if fewer than four bytes are available
     */
    abstract int readInt() throws IOException;

    /**
     * Reads the next {@code length} bytes into a fresh array.
     *
     * @implSpec
     * Subclasses must return a freshly allocated array of exactly
     * {@code length} bytes copied from the source and advance the
     * source position by {@code length}.
     *
     * @param length the number of bytes to read
     * @return a newly allocated array holding the bytes that were
     *         read
     * @throws IOException if fewer than {@code length} bytes are
     *         available
     */
    abstract byte[] readBytes(int length) throws IOException;

    /**
     * Consumes and discards any bytes of the current stanza's frame that
     * {@link #decode()} did not read.
     *
     * <p>Relevant only to the {@link #fromStream(InputStream, int) length-bounded
     * stream decoder}, whose {@code source} is a continuous multi-frame stream: a
     * stanza that decodes to fewer bytes than its frame carries (trailing bytes,
     * or a decode that failed part way through) leaves the source mid-frame, and
     * this drains the rest so the next read starts on the following frame's length
     * prefix. Draining also lets the transport layer finish authenticating the
     * frame it streamed.
     *
     * @implSpec
     * A fully-buffered source ({@code byte[]}, {@link ByteBuffer},
     * {@link MemorySegment}) is already bounded to exactly one stanza, so its
     * implementation does nothing. A stream-backed source drains the frame bytes
     * {@link #decode()} left unread.
     *
     * @throws IOException if the source ends before the frame is fully consumed,
     *         which leaves the frame unauthenticated and is a reconnect-worthy
     *         fault for the caller
     */
    public abstract void drain() throws IOException;

    /**
     * Closes this decoder, releasing any resources it holds.
     *
     * <p>The default implementation is a no-op because the buffer-backed
     * uncompressed subclasses hold no resources beyond the caller-owned
     * source. Compressed subclasses override to release the {@link Inflater}.
     * The source is always caller-owned and is never closed here.
     *
     * @implNote
     * This implementation narrows the throws clause from {@link Exception} on
     * {@link AutoCloseable#close()} to {@link IOException} so callers do not
     * need a redundant outer catch.
     *
     * @throws IOException if the underlying source fails to close
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * Reads a complete stanza from the source.
     *
     * <p>Walks the size header, description, attribute pairs, and content slot
     * of one stanza, recursing into child stanzas through {@link #readList(int)}.
     *
     * @return the decoded stanza
     * @throws IOException if the stream is truncated or carries a malformed tag
     */
    private Stanza readStanza() throws IOException {
        var size = readStanzaSize();
        if (size == 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed stanza: zero size header");
            throw new IOException("Unexpected empty stanza");
        }

        var description = readString();
        var attrs = readAttributes(size - 1);

        if ((size & 1) == 1) {
            return new Stanza.EmptyStanza(description, attrs);
        }

        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> new Stanza.EmptyStanza(description, attrs);
            case JID_INTEROP -> new Stanza.JidStanza(description, attrs, readInteropJid());
            case JID_FB -> new Stanza.JidStanza(description, attrs, readFbJid());
            case AD_JID -> new Stanza.JidStanza(description, attrs, readAdJid());
            case LIST_8 -> new Stanza.ContainerStanza(description, attrs, readList8());
            case LIST_16 -> new Stanza.ContainerStanza(description, attrs, readList16());
            case JID_PAIR -> new Stanza.JidStanza(description, attrs, readJidPair());
            case HEX_8 -> new Stanza.TextStanza(description, attrs, readPacked(HEX_ALPHABET));
            case BINARY_8 -> new Stanza.BytesStanza(description, attrs, readBinary8());
            case BINARY_20 -> new Stanza.BytesStanza(description, attrs, readBinary20());
            case BINARY_32 -> new Stanza.BytesStanza(description, attrs, readBinary32());
            case NIBBLE_8 -> new Stanza.TextStanza(description, attrs, readPacked(NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new Stanza.TextStanza(description, attrs, readDictionaryToken(DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new Stanza.TextStanza(description, attrs, readDictionaryToken(DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new Stanza.TextStanza(description, attrs, readDictionaryToken(DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new Stanza.TextStanza(description, attrs, readDictionaryToken(DICTIONARY_3_TOKENS));
            default -> {
                var index = tag & 0xFF;
                if (index >= 240) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed stanza content tag {0}", index);
                    throw new IOException("Unexpected tag in stanza content: " + index);
                }
                yield new Stanza.TextStanza(description, attrs, readSingleByteToken(tag));
            }
        };
    }

    /**
     * Reads a list size header.
     *
     * <p>The size encodes one slot for the description, two slots per attribute
     * (key plus value), and one optional slot for the content. An odd size
     * signals a content-less {@link Stanza.EmptyStanza}.
     *
     * @return the list size in slot units
     * @throws IOException if reading fails
     * @throws IllegalStateException if the leading byte is not a known list
     *         size tag
     */
    private int readStanzaSize() throws IOException {
        var token = (byte) read();
        return switch (token) {
            case LIST_8 -> read() & 0xFF;
            case LIST_16 -> readShort();
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed stanza size token {0}", token);
                throw new IllegalStateException("Unexpected value: " + token);
            }
        };
    }

    /**
     * Reads a string under whichever encoding tag appears next.
     *
     * <p>Used for descriptions, attribute keys, and any attribute or content
     * slot that decodes to text. A {@link StanzaTags#LIST_EMPTY} tag yields
     * {@code null}, which the caller filters out where appropriate.
     *
     * @return the decoded string, or {@code null} when the leading tag is
     *         {@link StanzaTags#LIST_EMPTY}
     * @throws IOException if the leading tag is not a string shape
     */
    private String readString() throws IOException {
        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case HEX_8 -> readPacked(HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8(), StandardCharsets.UTF_8);
            case BINARY_20 -> new String(readBinary20(), StandardCharsets.UTF_8);
            case BINARY_32 -> new String(readBinary32(), StandardCharsets.UTF_8);
            case DICTIONARY_0 -> readDictionaryToken(DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(DICTIONARY_3_TOKENS);
            default -> {
                var index = tag & 0xFF;
                if (index >= 240) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed string tag {0}", index);
                    throw new IOException("Unexpected tag in string position: " + index);
                }
                yield readSingleByteToken(tag);
            }
        };
    }

    /**
     * Reads a binary blob with an 8-bit length prefix.
     *
     * @return the bytes that were read
     * @throws IOException if the stream is truncated
     */
    private byte[] readBinary8() throws IOException {
        var size = read() & 0xFF;
        return readBytes(size);
    }

    /**
     * Reads a binary blob with a 20-bit big-endian length prefix.
     *
     * <p>The 20-bit width is packed into three bytes: the low nibble of the
     * first byte holds bits 16-19, and the next two bytes hold the low 16
     * bits.
     *
     * @return the bytes that were read
     * @throws IOException if the stream is truncated
     */
    private byte[] readBinary20() throws IOException {
        var size = ((read() & 0x0F) << 16)
                   | (read() << 8)
                   | read();
        return readBytes(size);
    }

    /**
     * Reads a binary blob with a 32-bit big-endian length prefix.
     *
     * @return the bytes that were read
     * @throws IOException if the stream is truncated
     */
    private byte[] readBinary32() throws IOException {
        return readBytes(readInt());
    }

    /**
     * Reads an 8-bit token index and resolves it through the supplied
     * extension dictionary.
     *
     * @param dictionary the dictionary to look up
     * @return the resolved string
     * @throws IOException if the stream is truncated
     */
    private String readDictionaryToken(StanzaTokens dictionary) throws IOException {
        var index = read() & 0xFF;
        return dictionary.get(index);
    }

    /**
     * Resolves a single-byte token through
     * {@link StanzaTokens#SINGLE_BYTE_TOKENS}.
     *
     * <p>The tag byte itself is the index; no further bytes are consumed.
     *
     * @param tag the token byte
     * @return the resolved string
     */
    private String readSingleByteToken(byte tag) {
        var index = tag & 0xFF;
        return SINGLE_BYTE_TOKENS.get(index);
    }

    /**
     * Reads {@code size / 2} attribute key value pairs preserving their
     * declaration order.
     *
     * <p>The {@code size} is the slot count reported by the enclosing stanza's
     * size header minus one for the description; each pair consumes two slots
     * so the loop subtracts two per iteration.
     *
     * @param size the number of slots that the attribute block consumes
     *             (always even)
     * @return the parsed attribute map
     * @throws IOException if the stream is truncated
     */
    private SequencedMap<String, StanzaAttribute> readAttributes(int size) throws IOException {
        var attributes = new LinkedHashMap<String, StanzaAttribute>(size / 2);
        while (size >= 2) {
            var key = readString();
            var value = readAttribute();
            attributes.put(key, value);
            size -= 2;
        }
        return attributes;
    }

    /**
     * Reads a single attribute value under whichever encoding tag appears
     * next.
     *
     * <p>Attribute values appear in the same wire shapes as content slots; the
     * {@link StanzaTags#LIST_EMPTY} tag and the list shapes
     * ({@link StanzaTags#LIST_8}, {@link StanzaTags#LIST_16}) yield {@code null}
     * after their bodies are consumed, for forward compatibility with future
     * protocol revisions that may carry richer attribute structures.
     *
     * @return the parsed attribute, or {@code null} for
     *         {@link StanzaTags#LIST_EMPTY} and list shapes
     * @throws IOException if the stream is truncated or holds a malformed tag
     */
    private StanzaAttribute readAttribute() throws IOException {
        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case JID_INTEROP -> new StanzaAttribute.JidAttribute(readInteropJid());
            case JID_FB -> new StanzaAttribute.JidAttribute(readFbJid());
            case AD_JID -> new StanzaAttribute.JidAttribute(readAdJid());
            case LIST_8 -> {
                readList8();
                yield null;
            }
            case LIST_16 -> {
                readList16();
                yield null;
            }
            case JID_PAIR -> new StanzaAttribute.JidAttribute(readJidPair());
            case HEX_8 -> new StanzaAttribute.TextAttribute(readPacked(HEX_ALPHABET));
            case BINARY_8 -> new StanzaAttribute.BytesAttribute(readBinary8());
            case BINARY_20 -> new StanzaAttribute.BytesAttribute(readBinary20());
            case BINARY_32 -> new StanzaAttribute.BytesAttribute(readBinary32());
            case NIBBLE_8 -> new StanzaAttribute.TextAttribute(readPacked(NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new StanzaAttribute.TextAttribute(readDictionaryToken(DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new StanzaAttribute.TextAttribute(readDictionaryToken(DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new StanzaAttribute.TextAttribute(readDictionaryToken(DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new StanzaAttribute.TextAttribute(readDictionaryToken(DICTIONARY_3_TOKENS));
            default -> {
                var index = tag & 0xFF;
                if (index >= 240) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed attribute tag {0}", index);
                    throw new IOException("Unexpected tag in attribute position: " + index);
                }
                yield new StanzaAttribute.TextAttribute(readSingleByteToken(tag));
            }
        };
    }

    /**
     * Reads a list of child stanzas with an 8-bit length prefix.
     *
     * @return the parsed children in declaration order
     * @throws IOException if the stream is truncated
     */
    private SequencedCollection<Stanza> readList8() throws IOException {
        var length = read() & 0xFF;
        return readList(length);
    }

    /**
     * Reads a list of child stanzas with a 16-bit big-endian length
     * prefix.
     *
     * @return the parsed children in declaration order
     * @throws IOException if the stream is truncated
     */
    private SequencedCollection<Stanza> readList16() throws IOException {
        return readList(readShort());
    }

    /**
     * Reads {@code size} consecutive child stanzas.
     *
     * @param size the number of stanzas to read
     * @return the parsed children in declaration order
     * @throws IOException if the stream is truncated
     */
    private SequencedCollection<Stanza> readList(int size) throws IOException {
        var results = new ArrayList<Stanza>(size);
        for (var index = 0; index < size; index++) {
            var stanza = readStanza();
            results.add(stanza);
        }
        return results;
    }

    /**
     * Reads a packed string under the supplied 16-entry alphabet.
     *
     * <p>Packed strings store two characters per byte; the leading length
     * byte's high bit signals whether the last byte holds one character (and a
     * filler nibble) or two.
     *
     * @param alphabet the 16-entry alphabet to translate nibbles through
     * @return the decoded string
     * @throws IOException if the stream is truncated
     */
    private String readPacked(char[] alphabet) throws IOException {
        var token = read() & 0xFF;
        var start = token >>> 7;
        var end = token & 127;
        var string = new char[2 * end - start];
        for (var index = 0; index < string.length - 1; index += 2) {
            token = read() & 0xFF;
            string[index] = alphabet[token >>> 4];
            string[index + 1] = alphabet[15 & token];
        }
        if (start != 0) {
            token = read() & 0xFF;
            string[string.length - 1] = alphabet[token >>> 4];
        }
        return String.valueOf(string);
    }

    /**
     * Reads a {@link StanzaTags#JID_PAIR} body.
     *
     * <p>The body is {@code (user, server)}; a {@link StanzaTags#LIST_EMPTY} in
     * the user slot becomes a server-only JID via {@link Jid#of(JidServer)}.
     *
     * @return the parsed JID
     * @throws IOException if the stream is truncated
     * @throws NullPointerException if the server component is missing
     */
    private Jid readJidPair() throws IOException {
        var user = readString();
        var server = JidServer.of(Objects.requireNonNull(readString(), "Malformed value pair: no server"));
        return user == null ? Jid.of(server) : Jid.of(user, server);
    }

    /**
     * Reads an {@link StanzaTags#AD_JID} body.
     *
     * <p>The body is {@code (domain:u8, device:u8, user)}. The domain byte
     * decodes through the {@code DOMAIN_*} constants; the hosted branch
     * accepts any byte whose low bit is clear and high bit is set.
     *
     * @return the parsed JID
     * @throws IOException if the stream is truncated or carries an unknown
     *         domain code
     */
    private Jid readAdJid() throws IOException {
        var domainType = read() & 0xFF;
        var device = read() & 0xFF;
        var user = readString();
        var server = switch (domainType) {
            case DOMAIN_WHATSAPP -> JidServer.user();
            case DOMAIN_LID -> JidServer.lid();
            case DOMAIN_HOSTED_LID -> JidServer.hostedLid();
            default -> {
                if ((domainType & 1) == 0 && (domainType & DOMAIN_HOSTED) != 0) {
                    yield JidServer.hosted();
                }
                if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed ad-jid domain type {0}", domainType);
                throw new IOException("Invalid AD_JID domain type: " + domainType);
            }
        };
        return Jid.of(user, server, device, 0);
    }

    /**
     * Reads a {@link StanzaTags#JID_FB} body.
     *
     * <p>The body is {@code (user, device:u16, server)}; the server is consumed
     * and discarded since the Messenger server is fixed at
     * {@link JidServer#messenger()}.
     *
     * @return the parsed JID
     * @throws IOException if the stream is truncated
     */
    private Jid readFbJid() throws IOException {
        var user = readString();
        var device = readShort();
        var _ = readString();
        return Jid.of(user, JidServer.messenger(), device, 0);
    }

    /**
     * Reads a {@link StanzaTags#JID_INTEROP} body.
     *
     * <p>The body is {@code (user, device:u16, integrator:u16, server)}; the
     * resulting user component is the {@code "integrator-user"} compound form
     * that bridges the integrator id back into the JID surface, and the
     * consumed server string is discarded.
     *
     * @return the parsed JID
     * @throws IOException if the stream is truncated
     */
    private Jid readInteropJid() throws IOException {
        var user = readString();
        var device = readShort();
        var integrator = readShort();
        var _ = readString();
        return Jid.of(integrator + "-" + user, JidServer.interop(), device, 0);
    }

    /**
     * Decodes from a caller-supplied {@code byte[]} without decompressing.
     *
     * <p>Used when the whole stanza is already in memory as a fresh array; the
     * read primitives index the array directly.
     */
    private static final class ByteArrayUncompressed extends StanzaReader {

        /**
         * Holds the source byte array.
         */
        private final byte[] source;

        /**
         * Holds the current read position into {@link #source}.
         */
        private int position;

        /**
         * Constructs a new {@code byte[]}-backed uncompressed decoder.
         *
         * @param source the source bytes
         * @param offset the starting read offset
         */
        ByteArrayUncompressed(byte[] source, int offset) {
            this.source = source;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasData() {
            return position < source.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int read() throws IOException {
            if (position >= source.length) {
                throw new IOException("Unexpected end of data");
            }
            return source[position++] & 0xFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readShort() throws IOException {
            if (source.length - position < 2) {
                throw new IOException("Unexpected end of data");
            }
            var value = DataUtils.getShort(source, position, ByteOrder.BIG_ENDIAN);
            position += 2;
            return value & 0xFFFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readInt() throws IOException {
            if (source.length - position < 4) {
                throw new IOException("Unexpected end of data");
            }
            var value = DataUtils.getInt(source, position, ByteOrder.BIG_ENDIAN);
            position += 4;
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        byte[] readBytes(int length) throws IOException {
            if (source.length - position < length) {
                throw new IOException("Insufficient data available");
            }
            var result = new byte[length];
            System.arraycopy(source, position, result, 0, length);
            position += length;
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation does nothing: the array is already bounded to one
         * stanza, so no continuous-stream cursor needs advancing.
         */
        @Override
        public void drain() {

        }
    }

    /**
     * Decodes from a caller-supplied {@link ByteBuffer} without decompressing,
     * advancing the buffer's position as bytes are consumed.
     *
     * <p>Forces big-endian order on the buffer at construction so subsequent
     * {@link ByteBuffer#getShort()} and {@link ByteBuffer#getInt()} calls read
     * multi-byte values in the wire's byte order.
     */
    private static final class ByteBufferUncompressed extends StanzaReader {

        /**
         * Holds the source buffer.
         */
        private final ByteBuffer source;

        /**
         * Constructs a new {@link ByteBuffer}-backed uncompressed decoder.
         *
         * <p>Forces big-endian order on the buffer so multi-byte primitives
         * read in wire order.
         *
         * @param source the source buffer
         */
        ByteBufferUncompressed(ByteBuffer source) {
            this.source = source.order(ByteOrder.BIG_ENDIAN);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasData() {
            return source.hasRemaining();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int read() throws IOException {
            if (!source.hasRemaining()) {
                throw new IOException("Unexpected end of data");
            }
            return source.get() & 0xFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readShort() throws IOException {
            if (source.remaining() < 2) {
                throw new IOException("Unexpected end of data");
            }
            return source.getShort() & 0xFFFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readInt() throws IOException {
            if (source.remaining() < 4) {
                throw new IOException("Unexpected end of data");
            }
            return source.getInt();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        byte[] readBytes(int length) throws IOException {
            if (source.remaining() < length) {
                throw new IOException("Insufficient data available");
            }
            var result = new byte[length];
            source.get(result);
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation does nothing: the buffer is already bounded to one
         * stanza, so no continuous-stream cursor needs advancing.
         */
        @Override
        public void drain() {

        }
    }

    /**
     * Decodes from a caller-supplied {@link MemorySegment} without
     * decompressing, advancing an internal position cursor as bytes are
     * consumed.
     *
     * <p>Used when the stanza sits inside a foreign memory segment (off-heap
     * socket buffer, mapped file). The FFM-based reads avoid a {@code byte[]}
     * round trip.
     */
    private static final class MemorySegmentUncompressed extends StanzaReader {

        /**
         * Holds the big-endian short layout reused across reads.
         */
        private static final ValueLayout.OfShort BE_SHORT =
                ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

        /**
         * Holds the big-endian int layout reused across reads.
         */
        private static final ValueLayout.OfInt BE_INT =
                ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

        /**
         * Holds the source segment.
         */
        private final MemorySegment source;

        /**
         * Holds the current read offset within {@link #source}.
         */
        private long position;

        /**
         * Constructs a new {@link MemorySegment}-backed uncompressed
         * decoder.
         *
         * @param source the source segment
         * @param offset the starting byte offset
         */
        MemorySegmentUncompressed(MemorySegment source, long offset) {
            this.source = source;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasData() {
            return position < source.byteSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int read() throws IOException {
            if (position >= source.byteSize()) {
                throw new IOException("Unexpected end of data");
            }
            return source.get(ValueLayout.JAVA_BYTE, position++) & 0xFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readShort() throws IOException {
            if (source.byteSize() - position < 2) {
                throw new IOException("Unexpected end of data");
            }
            var value = source.get(BE_SHORT, position);
            position += 2;
            return value & 0xFFFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readInt() throws IOException {
            if (source.byteSize() - position < 4) {
                throw new IOException("Unexpected end of data");
            }
            var value = source.get(BE_INT, position);
            position += 4;
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        byte[] readBytes(int length) throws IOException {
            if (source.byteSize() - position < length) {
                throw new IOException("Insufficient data available");
            }
            var result = new byte[length];
            MemorySegment.copy(source, ValueLayout.JAVA_BYTE, position, result, 0, length);
            position += length;
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation does nothing: the segment is already bounded to one
         * stanza, so no continuous-stream cursor needs advancing.
         */
        @Override
        public void drain() {

        }
    }

    /**
     * Decodes from an {@link InputStream} without decompressing.
     *
     * <p>A per-instance staging buffer absorbs bulk reads from the underlying
     * stream so the primitive reads serve from memory and only trigger an
     * {@link InputStream#read(byte[])} when the buffer drains; wrapping the
     * input in {@link java.io.BufferedInputStream} is therefore unnecessary.
     */
    private static final class StreamUncompressed extends StanzaReader {

        /**
         * Holds the source input stream.
         */
        private final InputStream source;

        /**
         * Holds the number of bytes of the current frame not yet pulled from
         * {@link #source}.
         *
         * <p>Every read from {@code source} is clamped to this count so the
         * decoder never crosses into the following frame of a continuous
         * multi-frame stream; once it reaches zero the frame is exhausted.
         */
        private int frameRemaining;

        /**
         * Holds the bytes pulled from {@link #source} pending consumption.
         */
        private final byte[] buffer;

        /**
         * Holds the read offset into {@link #buffer}.
         */
        private int bufferPosition;

        /**
         * Holds the count of valid bytes in {@link #buffer}.
         */
        private int bufferLimit;

        /**
         * Marks that {@link InputStream#read(byte[], int, int)} has returned
         * end-of-stream so subsequent reads short-circuit to an end-of-data
         * error without re-querying the stream.
         */
        private boolean exhausted;

        /**
         * Constructs a new {@link InputStream}-backed uncompressed decoder
         * seeded with a buffer that the factory has already filled with the
         * leading bulk read.
         *
         * <p>The factory consumed the flags byte at index zero before
         * dispatching to this constructor, so {@link #bufferPosition} starts at
         * one.
         *
         * @param source          the source stream
         * @param buffer          the staging buffer; the factory has read into
         *                        it starting at index zero, with the flags byte
         *                        at index zero
         * @param prefilledBytes  the count of valid bytes at the head of
         *                        {@code buffer}; the first byte is the consumed
         *                        flags byte
         * @param frameRemaining  the number of frame bytes not yet pulled from
         *                        {@code source} after the prefill
         */
        StreamUncompressed(InputStream source, byte[] buffer, int prefilledBytes, int frameRemaining) {
            this.source = source;
            this.frameRemaining = frameRemaining;
            this.buffer = buffer;
            this.bufferPosition = 1;
            this.bufferLimit = prefilledBytes;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns {@code true} when the staging
         * buffer still has bytes; if drained, falls back to
         * {@link InputStream#available()} which is allowed to
         * return zero even when more bytes will become available
         * later, so callers should not rely on a {@code false}
         * return as a definitive end-of-stream signal.
         */
        @Override
        public boolean hasData() {
            if (bufferPosition < bufferLimit) {
                return true;
            }
            if (exhausted) {
                return false;
            }
            try {
                return source.available() > 0;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int read() throws IOException {
            if (bufferPosition >= bufferLimit) {
                fillBuffer();
                if (bufferPosition >= bufferLimit) {
                    throw new IOException("Unexpected end of data");
                }
            }
            return buffer[bufferPosition++] & 0xFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readShort() throws IOException {
            ensureAvailable(2);
            var value = DataUtils.getShort(buffer, bufferPosition, ByteOrder.BIG_ENDIAN);
            bufferPosition += 2;
            return value & 0xFFFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int readInt() throws IOException {
            ensureAvailable(4);
            var value = DataUtils.getInt(buffer, bufferPosition, ByteOrder.BIG_ENDIAN);
            bufferPosition += 4;
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        byte[] readBytes(int length) throws IOException {
            var result = new byte[length];
            var offset = 0;
            while (offset < length) {
                if (bufferPosition >= bufferLimit) {
                    fillBuffer();
                    if (bufferPosition >= bufferLimit) {
                        throw new IOException("Insufficient data available");
                    }
                }
                var available = bufferLimit - bufferPosition;
                var toRead = Math.min(available, length - offset);
                System.arraycopy(buffer, bufferPosition, result, offset, toRead);
                bufferPosition += toRead;
                offset += toRead;
            }
            return result;
        }

        /**
         * Refills the staging buffer with another block pulled from the
         * underlying stream, starting at offset zero.
         *
         * <p>Idempotent once the stream is exhausted; resets the buffer to
         * empty so the calling primitive reports end-of-data on the next
         * access.
         *
         * @throws IOException if the underlying stream fails
         */
        private void fillBuffer() throws IOException {
            if (exhausted || frameRemaining <= 0) {
                exhausted = true;
                bufferPosition = 0;
                bufferLimit = 0;
                return;
            }
            var n = source.read(buffer, 0, Math.min(buffer.length, frameRemaining));
            if (n < 0) {
                exhausted = true;
                bufferPosition = 0;
                bufferLimit = 0;
                return;
            }
            frameRemaining -= n;
            bufferPosition = 0;
            bufferLimit = n;
        }

        /**
         * Ensures that at least {@code needed} contiguous bytes are available
         * starting at {@link #bufferPosition}, compacting and refilling the
         * staging buffer as necessary.
         *
         * <p>Multi-byte primitive reads ({@link #readShort()},
         * {@link #readInt()}) need contiguous bytes; this helper shifts any
         * partial trailing bytes to the front of the buffer before refilling so
         * the next read can take them directly.
         *
         * @param needed the minimum number of contiguous bytes required
         * @throws IOException if the source ends before enough bytes are
         *         buffered
         */
        private void ensureAvailable(int needed) throws IOException {
            var available = bufferLimit - bufferPosition;
            if (available >= needed) {
                return;
            }
            if (available > 0) {
                System.arraycopy(buffer, bufferPosition, buffer, 0, available);
            }
            bufferPosition = 0;
            bufferLimit = available;
            while (bufferLimit < needed) {
                if (exhausted || frameRemaining <= 0) {
                    exhausted = true;
                    throw new IOException("Unexpected end of data");
                }
                var n = source.read(buffer, bufferLimit, Math.min(buffer.length - bufferLimit, frameRemaining));
                if (n < 0) {
                    exhausted = true;
                    throw new IOException("Unexpected end of data");
                }
                frameRemaining -= n;
                bufferLimit += n;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation discards the staging buffer and drains any frame
         * bytes not yet pulled from {@link #source}, reusing the staging buffer
         * as scratch so no frame-sized array is allocated.
         */
        @Override
        public void drain() throws IOException {
            bufferPosition = bufferLimit;
            while (frameRemaining > 0) {
                var n = source.read(buffer, 0, Math.min(buffer.length, frameRemaining));
                if (n < 0) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "datagram truncated, {0} byte(s) missing", frameRemaining);
                    throw new IOException("Datagram truncated: " + frameRemaining + " plaintext byte(s) missing");
                }
                frameRemaining -= n;
            }
            exhausted = true;
        }
    }

    /**
     * Inflates DEFLATE compressed source bytes through a staging buffer.
     *
     * <p>Centralises the {@link Inflater} lifecycle, the inflated-byte staging
     * buffer, and the per-primitive read and inflate logic. Subclasses provide
     * only the source-specific {@link #fillInflaterInput(byte[], int)} that
     * copies raw compressed bytes from the underlying source into the inflater
     * input buffer.
     */
    private abstract static class Compressed extends StanzaReader {

        /**
         * Holds the inflater that decompresses the source bytes.
         */
        private final Inflater inflater;

        /**
         * Holds the inflated bytes pending consumption.
         */
        private final byte[] decompressionBuffer;

        /**
         * Holds the compressed bytes fed into the inflater.
         */
        final byte[] inflaterInputBuffer;

        /**
         * Holds the read offset into {@link #decompressionBuffer}.
         */
        private int bufferPosition;

        /**
         * Holds the count of valid bytes in {@link #decompressionBuffer}.
         */
        private int bufferLimit;

        /**
         * Allocates a new inflater and two equally sized staging buffers.
         *
         * @param bufferSize the capacity of the inflater input and inflated
         *                   output staging buffers, in bytes
         */
        Compressed(int bufferSize) {
            this.inflater = new Inflater();
            this.decompressionBuffer = new byte[bufferSize];
            this.inflaterInputBuffer = new byte[bufferSize];
        }

        /**
         * Adopts a caller-supplied inflater input buffer that already holds
         * some compressed bytes, seeding the inflater with the supplied range.
         *
         * <p>Used by the stream-backed variant when the factory has already
         * pulled the leading bulk read; avoids a second allocation and a copy
         * into a freshly sized inflater buffer.
         *
         * @param prefilledInflaterInput the inflater input buffer; its length
         *                               determines the size of the inflated
         *                               output staging buffer
         * @param prefillOffset          start of the pre-filled compressed-bytes
         *                               range (inclusive)
         * @param prefillLimit           end of the pre-filled compressed-bytes
         *                               range (exclusive)
         */
        Compressed(byte[] prefilledInflaterInput, int prefillOffset, int prefillLimit) {
            this.inflater = new Inflater();
            this.decompressionBuffer = new byte[prefilledInflaterInput.length];
            this.inflaterInputBuffer = prefilledInflaterInput;
            if (prefillLimit > prefillOffset) {
                inflater.setInput(prefilledInflaterInput, prefillOffset, prefillLimit - prefillOffset);
            }
        }

        /**
         * Copies up to {@code max} compressed bytes from the
         * source-specific backing into {@link #inflaterInputBuffer}
         * starting at offset zero.
         *
         * @implSpec
         * Subclasses must copy at most {@code max} bytes from the
         * underlying source into {@code dst} starting at index zero
         * and return the number of bytes actually copied, or zero
         * when no more source bytes are available.
         *
         * @param dst the destination buffer (always
         *            {@link #inflaterInputBuffer})
         * @param max the maximum number of bytes to copy
         * @return the number of bytes copied, or {@code 0} if no more
         *         source bytes are available
         */
        abstract int fillInflaterInput(byte[] dst, int max);

        /**
         * Returns whether more compressed source bytes are available
         * to feed the inflater.
         *
         * @implSpec
         * Subclasses return {@code true} when at least one
         * compressed byte can still be drawn from the underlying
         * source and {@code false} once the source is exhausted.
         *
         * @return {@code true} when the source still has compressed
         *         bytes to feed
         */
        abstract boolean sourceHasRemaining();

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns {@code true} when either the
         * staging buffer still has bytes, the inflater has not
         * finished, or the source still has compressed bytes to
         * feed; any one of the three implies more output is
         * reachable.
         */
        @Override
        public final boolean hasData() {
            return bufferPosition < bufferLimit
                   || !inflater.finished()
                   || sourceHasRemaining();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        final int read() throws IOException {
            if (bufferPosition >= bufferLimit) {
                fillDecompressionBuffer();
            }

            if (bufferPosition >= bufferLimit) {
                throw new IOException("Unexpected end of decompressed data");
            }

            return decompressionBuffer[bufferPosition++] & 0xFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        final int readShort() throws IOException {
            ensureAvailable(2);
            var value = DataUtils.getShort(decompressionBuffer, bufferPosition, ByteOrder.BIG_ENDIAN);
            bufferPosition += 2;
            return value & 0xFFFF;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        final int readInt() throws IOException {
            ensureAvailable(4);
            var value = DataUtils.getInt(decompressionBuffer, bufferPosition, ByteOrder.BIG_ENDIAN);
            bufferPosition += 4;
            return value;
        }

        /**
         * Ensures that at least {@code needed} contiguous inflated bytes are
         * available starting at {@link #bufferPosition}.
         *
         * <p>Multi-byte primitive reads need contiguous bytes; this helper
         * compacts any trailing partial bytes and then inflates more until
         * either the count is reached or the inflater reports end-of-stream.
         *
         * @param needed the minimum number of contiguous bytes required
         * @throws IOException if the source ends before enough bytes are
         *         inflated
         */
        private void ensureAvailable(int needed) throws IOException {
            var available = bufferLimit - bufferPosition;
            if (available >= needed) {
                return;
            }
            if (available > 0) {
                System.arraycopy(decompressionBuffer, bufferPosition, decompressionBuffer, 0, available);
            }
            bufferPosition = 0;
            bufferLimit = available;
            try {
                while (bufferLimit < needed) {
                    // Never pull more compressed input once this stanza's stream is finished: a
                    // stream source (StreamCompressed) hides datagram boundaries, so refilling
                    // here would read into the next datagram and desync the reader cursor.
                    if (inflater.needsInput() && !inflater.finished() && sourceHasRemaining()) {
                        var toRead = fillInflaterInput(inflaterInputBuffer, inflaterInputBuffer.length);
                        if (toRead > 0) {
                            inflater.setInput(inflaterInputBuffer, 0, toRead);
                        }
                    }
                    var inflated = inflater.inflate(decompressionBuffer, bufferLimit, decompressionBuffer.length - bufferLimit);
                    if (inflated == 0) {
                        throw new IOException("Unexpected end of decompressed data");
                    }
                    bufferLimit += inflated;
                }
            } catch (DataFormatException e) {
                throw new IOException("Decompression error", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        final byte[] readBytes(int length) throws IOException {
            var result = new byte[length];
            var offset = 0;
            while (offset < length) {
                if (bufferPosition >= bufferLimit) {
                    fillDecompressionBuffer();
                }

                if (bufferPosition >= bufferLimit) {
                    throw new IOException("Unexpected end of decompressed data");
                }

                var available = bufferLimit - bufferPosition;
                var toRead = Math.min(available, length - offset);
                System.arraycopy(decompressionBuffer, bufferPosition, result, offset, toRead);
                bufferPosition += toRead;
                offset += toRead;
            }
            return result;
        }

        /**
         * Refills the staging buffer with another inflated block.
         *
         * <p>Feeds the inflater more compressed bytes if it has drained its
         * input and the source still has more, then inflates one block into the
         * staging buffer.
         *
         * @throws IOException if the source bytes are malformed
         */
        private void fillDecompressionBuffer() throws IOException {
            try {
                // Stop refilling once this stanza's stream is finished; otherwise a stream source,
                // whose sourceHasRemaining() spans datagram boundaries, would read into the next
                // datagram and leave the reader cursor misaligned for the following stanza.
                if (inflater.needsInput() && !inflater.finished() && sourceHasRemaining()) {
                    var available = fillInflaterInput(inflaterInputBuffer, inflaterInputBuffer.length);
                    if (available > 0) {
                        inflater.setInput(inflaterInputBuffer, 0, available);
                    }
                }

                bufferPosition = 0;
                bufferLimit = inflater.inflate(decompressionBuffer);
            } catch (DataFormatException e) {
                throw new IOException("Decompression error", e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation does nothing: a fully-buffered compressed source is
         * bounded to one stanza, so no continuous-stream cursor needs advancing.
         * {@link StreamCompressed} overrides this to drain a frame's unread bytes.
         */
        @Override
        public void drain() throws IOException {

        }

        /**
         * Releases the {@link Inflater} held by this decoder.
         *
         * @throws IOException never thrown by the base implementation;
         *         declared for the narrowed
         *         {@link StanzaReader#close()} signature
         */
        @Override
        public void close() throws IOException {
            inflater.close();
        }
    }

    /**
     * Inflates from a {@code byte[]} source.
     */
    private static final class ByteArrayCompressed extends Compressed {

        /**
         * Holds the source byte array of compressed bytes.
         */
        private final byte[] source;

        /**
         * Holds the current read position into {@link #source}.
         */
        private int position;

        /**
         * Constructs a new {@code byte[]}-backed compressed decoder.
         *
         * @param source the source bytes
         * @param offset the starting read offset
         */
        ByteArrayCompressed(byte[] source, int offset) {
            super(DEFAULT_BUFFER_SIZE);
            this.source = source;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean sourceHasRemaining() {
            return position < source.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int fillInflaterInput(byte[] dst, int max) {
            var available = source.length - position;
            if (available <= 0) {
                return 0;
            }
            var toCopy = Math.min(available, max);
            System.arraycopy(source, position, dst, 0, toCopy);
            position += toCopy;
            return toCopy;
        }
    }

    /**
     * Inflates from a {@link ByteBuffer} source.
     */
    private static final class ByteBufferCompressed extends Compressed {

        /**
         * Holds the source buffer of compressed bytes.
         */
        private final ByteBuffer source;

        /**
         * Constructs a new {@link ByteBuffer}-backed compressed
         * decoder.
         *
         * @param source the source buffer
         */
        ByteBufferCompressed(ByteBuffer source) {
            super(DEFAULT_BUFFER_SIZE);
            this.source = source;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean sourceHasRemaining() {
            return source.hasRemaining();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int fillInflaterInput(byte[] dst, int max) {
            var toCopy = Math.min(source.remaining(), max);
            if (toCopy == 0) {
                return 0;
            }
            source.get(dst, 0, toCopy);
            return toCopy;
        }
    }

    /**
     * Inflates from a {@link MemorySegment} source.
     */
    private static final class MemorySegmentCompressed extends Compressed {

        /**
         * Holds the source segment of compressed bytes.
         */
        private final MemorySegment source;

        /**
         * Holds the current read offset within {@link #source}.
         */
        private long position;

        /**
         * Constructs a new {@link MemorySegment}-backed compressed
         * decoder.
         *
         * @param source the source segment
         * @param offset the starting byte offset
         */
        MemorySegmentCompressed(MemorySegment source, long offset) {
            super(DEFAULT_BUFFER_SIZE);
            this.source = source;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean sourceHasRemaining() {
            return position < source.byteSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int fillInflaterInput(byte[] dst, int max) {
            var available = source.byteSize() - position;
            if (available <= 0) {
                return 0;
            }
            var toCopy = (int) Math.min(available, max);
            MemorySegment.copy(source, ValueLayout.JAVA_BYTE, position, dst, 0, toCopy);
            position += toCopy;
            return toCopy;
        }
    }

    /**
     * Inflates from an {@link InputStream} source.
     */
    private static final class StreamCompressed extends Compressed {

        /**
         * Holds the source input stream of compressed bytes.
         */
        private final InputStream source;

        /**
         * Holds the number of bytes of the current frame not yet pulled from
         * {@link #source}.
         *
         * <p>Every read from {@code source} is clamped to this count so the
         * inflater is never fed compressed bytes belonging to the following
         * frame of a continuous multi-frame stream; once it reaches zero the
         * frame is exhausted.
         */
        private int frameRemaining;

        /**
         * Constructs a new {@link InputStream}-backed compressed decoder seeded
         * with a buffer that the factory has already filled with the leading
         * bulk read.
         *
         * <p>The factory consumed the flags byte at index zero before
         * dispatching to this constructor, so the base class seeds the inflater
         * starting at offset one.
         *
         * @param source                 the source stream
         * @param prefilledInflaterInput the inflater input buffer; the factory
         *                               has read into it starting at index
         *                               zero, with the flags byte at index zero
         *                               and compressed bytes from index one
         *                               onwards
         * @param prefilledBytes         the count of valid bytes at the head of
         *                               {@code prefilledInflaterInput}; the
         *                               first byte is the consumed flags byte
         * @param frameRemaining         the number of frame bytes not yet pulled
         *                               from {@code source} after the prefill
         */
        StreamCompressed(InputStream source, byte[] prefilledInflaterInput, int prefilledBytes, int frameRemaining) {
            super(prefilledInflaterInput, 1, prefilledBytes);
            this.source = source;
            this.frameRemaining = frameRemaining;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean sourceHasRemaining() {
            return frameRemaining > 0;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation clamps the read to the frame's remaining bytes so
         * the inflater is never fed input from the following frame. A transport
         * {@link IOException} or an end-of-stream returns {@code 0}, which surfaces
         * to the inflater as truncated compressed input; the reconnect-worthy
         * fault is then re-raised by {@link #drain()} when it re-reads the
         * unconsumed frame bytes. A datagram authentication failure arrives as an
         * unchecked exception and is not swallowed here.
         */
        @Override
        int fillInflaterInput(byte[] dst, int max) {
            if (frameRemaining <= 0) {
                return 0;
            }
            int n;
            try {
                n = source.read(dst, 0, Math.min(max, frameRemaining));
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "compressed stanza read failed, deferring to drain", e);
                return 0;
            }
            if (n < 0) {
                return 0;
            }
            frameRemaining -= n;
            return n;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation drains the frame's unread compressed bytes without
         * inflating them, reusing the inflater input buffer as scratch, so the
         * transport layer advances onto the next frame and finishes authenticating
         * this one.
         */
        @Override
        public void drain() throws IOException {
            while (frameRemaining > 0) {
                var n = source.read(inflaterInputBuffer, 0, Math.min(inflaterInputBuffer.length, frameRemaining));
                if (n < 0) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "datagram truncated, {0} byte(s) missing", frameRemaining);
                    throw new IOException("Datagram truncated: " + frameRemaining + " plaintext byte(s) missing");
                }
                frameRemaining -= n;
            }
        }
    }
}
