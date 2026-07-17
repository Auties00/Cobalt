package com.github.auties00.cobalt.stanza.binary;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaAttribute;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.function.Supplier;

import static com.github.auties00.cobalt.stanza.binary.StanzaTags.*;
import static com.github.auties00.cobalt.stanza.binary.StanzaTokens.*;

/**
 * Serialises {@link Stanza} trees into WhatsApp's compact binary stanza
 * format.
 *
 * <p>Outbound stanzas are produced through a {@link Stanza} tree (typically
 * built with {@link StanzaBuilder}); this
 * class converts the tree into the wire byte sequence consumed by the
 * WhatsApp socket. Strings pass through the {@link StanzaTokens}
 * compression ladder (single-byte token, extension dictionary, packed
 * nibble or hex, length-prefixed UTF-8); binary blobs use 8/20/32-bit
 * length prefixes; JIDs pick one of {@link StanzaTags#JID_PAIR},
 * {@link StanzaTags#AD_JID}, {@link StanzaTags#JID_INTEROP},
 * {@link StanzaTags#JID_FB} based on their server and device.
 *
 * <p>The class is an abstract template: the tree-traversal logic lives
 * here once and each subclass provides only the four primitive write
 * operations ({@link #writeByte(int)}, {@link #writeShort(int)},
 * {@link #writeInt(int)}, {@link #writeBytes(byte[], int, int)})
 * specialised for the backing sink. Four sinks are supported via
 * static factories:
 *
 * <ul>
 *   <li>{@link #toBytes(byte[], int)} writes into a caller-supplied
 *       {@code byte[]} starting at a given offset
 *   <li>{@link #toBuffer(ByteBuffer)} writes into a {@link ByteBuffer},
 *       advancing its position
 *   <li>{@link #toSegment(MemorySegment, long)} writes into a
 *       {@link MemorySegment} starting at the given byte offset
 *   <li>{@link #toStream(OutputStream)} streams each encoded byte
 *       directly to the underlying stream; the encoder allocates no
 *       intermediate buffer, so callers that want write batching
 *       should wrap the stream in
 *       {@link java.io.BufferedOutputStream} before passing it in
 * </ul>
 *
 * <p>Encoders are {@link AutoCloseable}: a buffer-backed encoder is a
 * no-op on close; the streaming encoder flushes the downstream when
 * the stream is caller-owned and closes it when the encoder owns it
 * (factory variant {@link #toStream(Supplier)}).
 *
 * <p>Pre-sizing a fixed sink through {@link StanzaSizer#sizeOf(Stanza)} lets most
 * stanzas serialise into a single allocation without any growth.
 *
 * @see Stanza
 * @see StanzaReader
 * @see StanzaSizer
 * @see StanzaTokens
 * @see StanzaTags
 */
@WhatsAppWebModule(moduleName = "WAWap")
public abstract class StanzaWriter implements AutoCloseable {

    /**
     * The logger for {@link StanzaWriter}.
     */
    private static final System.Logger LOGGER = Log.get(StanzaWriter.class);

    /**
     * Holds the exclusive upper bound for values that fit in an unsigned byte.
     *
     * <p>Serves as the decision threshold between {@link StanzaTags#LIST_8} and
     * {@link StanzaTags#LIST_16} for list sizes, and between
     * {@link StanzaTags#BINARY_8} and {@link StanzaTags#BINARY_20} for payload
     * lengths, mirroring the threshold in {@link StanzaSizer}.
     */
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;

    /**
     * Holds the exclusive upper bound for values that fit in an unsigned
     * short.
     *
     * <p>Serves as the decision threshold above which a list cannot be
     * encoded.
     */
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;

    /**
     * Holds the exclusive upper bound for values that fit in 20 bits.
     *
     * <p>Serves as the decision threshold between {@link StanzaTags#BINARY_20}
     * and {@link StanzaTags#BINARY_32} for payload lengths.
     */
    private static final int INT_20_MAX_VALUE = 1048576;

    /**
     * Constructs a new encoder for a package-internal subclass.
     *
     * <p>The hierarchy is closed within this package; the constructor exists
     * only to satisfy {@code javac} for the inner subclasses.
     */
    StanzaWriter() {

    }

    /**
     * Returns an encoder that writes a stanza into the supplied byte array
     * starting at {@code offset}.
     *
     * <p>The array must be at least {@code offset + StanzaSizer.sizeOf(stanza)}
     * bytes long, sized with {@link StanzaSizer#sizeOf(Stanza)} before allocating.
     * Encoders are single-use; each stanza needs a fresh one.
     *
     * @param output the destination byte array
     * @param offset the starting offset
     * @return a {@code byte[]}-backed encoder
     */
    public static StanzaWriter toBytes(byte[] output, int offset) {
        return new ByteArrayWriter(output, offset);
    }

    /**
     * Returns an encoder that writes a stanza into the supplied
     * {@link ByteBuffer} starting at its current position.
     *
     * <p>The buffer must have at least {@link StanzaSizer#sizeOf(Stanza)} bytes
     * remaining; the encoder advances the buffer's position as it writes.
     *
     * @param output the destination buffer
     * @return a {@link ByteBuffer}-backed encoder
     */
    public static StanzaWriter toBuffer(ByteBuffer output) {
        return new ByteBufferWriter(output);
    }

    /**
     * Returns an encoder that writes a stanza into the supplied
     * {@link MemorySegment} starting at {@code offset}.
     *
     * <p>The segment must have at least {@code offset + StanzaSizer.sizeOf(stanza)}
     * bytes of space. Used by callers writing into off-heap memory such as a
     * mapped file or a foreign socket buffer.
     *
     * @param output the destination segment
     * @param offset the starting byte offset within the segment
     * @return a {@link MemorySegment}-backed encoder
     */
    public static StanzaWriter toSegment(MemorySegment output, long offset) {
        return new MemorySegmentWriter(output, offset);
    }

    /**
     * Returns an encoder that streams a stanza directly to a caller-owned
     * {@link OutputStream}.
     *
     * <p>Every encoded token is forwarded to the underlying stream as it is
     * produced through single-byte and bulk {@code write} calls; no
     * intermediate buffer is allocated, so callers that want write batching
     * wrap the underlying stream in {@link java.io.BufferedOutputStream} before
     * passing it in. The encoder does not close {@code output} on
     * {@link #close()}; it only flushes any deferred bytes, leaving the caller
     * lifecycle control over the long-lived stream (typically a socket output
     * stream shared across many encoded stanzas). The {@link #toStream(Supplier)}
     * variant transfers stream ownership to the encoder.
     *
     * @param output the destination stream
     * @return an {@link OutputStream}-backed streaming encoder that flushes but
     *         does not close {@code output}
     */
    public static StanzaWriter toStream(OutputStream output) {
        return new StreamWriter(output, false);
    }

    /**
     * Returns an encoder that streams a stanza into an {@link OutputStream}
     * resolved from {@code supplier} and owned by the encoder.
     *
     * <p>The supplier is resolved eagerly at this call; the returned encoder
     * closes the resolved stream when {@link #close()} runs.
     *
     * @param supplier the source of the destination stream; resolved eagerly
     * @return an {@link OutputStream}-backed streaming encoder that closes the
     *         resolved stream
     */
    public static StanzaWriter toStream(Supplier<? extends OutputStream> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return new StreamWriter(supplier.get(), true);
    }

    /**
     * Writes one byte at the current encoder position.
     *
     * @implSpec
     * Subclasses must write the low 8 bits of {@code b} to the
     * sink and advance the position by one.
     *
     * @param b the byte value; only the low 8 bits are used
     * @throws IOException if the underlying sink rejects the write
     */
    abstract void writeByte(int b) throws IOException;

    /**
     * Writes two bytes at the current encoder position in big-endian
     * order.
     *
     * @implSpec
     * Subclasses must write the 16-bit value in big-endian order
     * and advance the position by two.
     *
     * @param v the 16-bit value to write
     * @throws IOException if the underlying sink rejects the write
     */
    abstract void writeShort(int v) throws IOException;

    /**
     * Writes four bytes at the current encoder position in big-endian
     * order.
     *
     * @implSpec
     * Subclasses must write the 32-bit value in big-endian order
     * and advance the position by four.
     *
     * @param v the 32-bit value to write
     * @throws IOException if the underlying sink rejects the write
     */
    abstract void writeInt(int v) throws IOException;

    /**
     * Writes {@code len} bytes from {@code src} starting at {@code off}
     * to the current encoder position.
     *
     * @implSpec
     * Subclasses must copy {@code len} bytes from {@code src}
     * starting at {@code off} into the sink and advance the
     * position by {@code len}.
     *
     * @param src the source array
     * @param off the starting offset in {@code src}
     * @param len the number of bytes to write
     * @throws IOException if the underlying sink rejects the write
     */
    abstract void writeBytes(byte[] src, int off, int len) throws IOException;

    /**
     * Closes this encoder, releasing any resources it holds and flushing any
     * deferred output to the underlying sink.
     *
     * <p>The default is a no-op because the buffer-backed subclasses hold no
     * resources beyond the caller-owned sink. The streaming subclass overrides
     * to flush a caller-owned stream or close an encoder-owned stream.
     *
     * @implNote
     * This implementation narrows the throws clause from {@link Exception} on
     * {@link AutoCloseable#close()} to {@link IOException}.
     *
     * @throws IOException if the underlying sink fails to close or flush
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * Writes the leading flags byte ({@code 0x00}, no compression) followed by
     * the supplied stanza's encoding.
     *
     * <p>The encode side always emits the uncompressed flags byte; compression
     * is only relevant for inbound stanzas. The companion
     * {@link StanzaSizer#sizeOf(Stanza)} accounts for this leading byte.
     *
     * @param stanza the stanza to encode
     * @throws IOException if the underlying sink rejects a write
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "makeStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void writeStanza(Stanza stanza) throws IOException {
        Objects.requireNonNull(stanza, "stanza");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "encoding stanza {0}", stanza);
        writeByte(0);
        writeStanzaBody(stanza);
    }

    /**
     * Writes a single stanza (size header, description, attributes, content)
     * without the leading flags byte.
     *
     * <p>Recurses from both {@link #writeStanza(Stanza)} and
     * {@link #writeChildren(SequencedCollection)}; the second caller has
     * already written the parent's flags byte.
     *
     * @param input the stanza to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeStanzaBody(Stanza input) throws IOException {
        writeList(input.size());
        writeString(input.description());
        writeAttributes(input.attributes());
        writeContent(input);
    }

    /**
     * Writes a list size tag followed by the size value, picking the narrowest
     * representation that fits.
     *
     * <p>Picks {@link StanzaTags#LIST_8} for sizes below 256 and
     * {@link StanzaTags#LIST_16} for sizes below 65536. Sizes at or above the
     * 16-bit limit are protocol overflows and throw.
     *
     * @param size the list size
     * @throws IllegalArgumentException if the size exceeds the 16-bit maximum
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeList(int size) throws IOException {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            writeByte(LIST_8);
            writeByte(size);
        } else if (size < UNSIGNED_SHORT_MAX_VALUE) {
            writeByte(LIST_16);
            writeShort(size);
        } else {
            throw new IllegalArgumentException("Cannot write list: overflow");
        }
    }

    /**
     * Writes a string under the most efficient applicable strategy.
     *
     * <p>The strategy ladder mirrors {@link StanzaSizer#stringLength(String)}
     * step-for-step: empty string ({@link StanzaTags#BINARY_8} plus
     * {@link StanzaTags#LIST_EMPTY}), single-byte token, each extension
     * dictionary in turn, packed nibble or hex for short strings
     * ({@code utf8Length < 128}) whose alphabet fits, and finally a
     * length-prefixed UTF-8 blob.
     *
     * @param input the string to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeString(String input) throws IOException {
        if (input.isEmpty()) {
            writeByte(BINARY_8);
            writeByte(LIST_EMPTY);
            return;
        }

        var singleByteTokenIndex = SINGLE_BYTE_TOKENS.indexOf(input);
        if (singleByteTokenIndex != -1) {
            writeByte(singleByteTokenIndex);
            return;
        }

        var dictionary0TokenIndex = DICTIONARY_0_TOKENS.indexOf(input);
        if (dictionary0TokenIndex != -1) {
            writeByte(DICTIONARY_0);
            writeByte(dictionary0TokenIndex);
            return;
        }

        var dictionary1TokenIndex = DICTIONARY_1_TOKENS.indexOf(input);
        if (dictionary1TokenIndex != -1) {
            writeByte(DICTIONARY_1);
            writeByte(dictionary1TokenIndex);
            return;
        }

        var dictionary2TokenIndex = DICTIONARY_2_TOKENS.indexOf(input);
        if (dictionary2TokenIndex != -1) {
            writeByte(DICTIONARY_2);
            writeByte(dictionary2TokenIndex);
            return;
        }

        var dictionary3TokenIndex = DICTIONARY_3_TOKENS.indexOf(input);
        if (dictionary3TokenIndex != -1) {
            writeByte(DICTIONARY_3);
            writeByte(dictionary3TokenIndex);
            return;
        }

        var utf8Length = StanzaSizer.calculateUtf8Length(input);
        if (utf8Length < 128) {
            var packedType = StanzaPackedFormat.getPackedType(input);
            if (packedType != -1) {
                writePacked(input, packedType);
                return;
            }
        }

        writeBinary(utf8Length);
        var encoded = input.getBytes(StandardCharsets.UTF_8);
        if (encoded.length != utf8Length) {
            throw new InternalError("Utf8 length mismatch");
        }
        writeBytes(encoded, 0, utf8Length);
    }

    /**
     * Writes a binary-blob length prefix tag followed by the length, using the
     * narrowest tag that fits.
     *
     * <p>Selects {@link StanzaTags#BINARY_8}, {@link StanzaTags#BINARY_20}, or
     * {@link StanzaTags#BINARY_32} for the same thresholds used in
     * {@link StanzaSizer#binaryLength(long)}.
     *
     * @param input the length value
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeBinary(int input) throws IOException {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            writeByte(BINARY_8);
            writeByte(input);
        } else if (input < INT_20_MAX_VALUE) {
            writeByte(BINARY_20);
            writeByte(input >> 16);
            writeByte(input >> 8);
            writeByte(input);
        } else {
            writeByte(BINARY_32);
            writeInt(input);
        }
    }

    /**
     * Writes a string in nibble or hex packed form (4 bits per character).
     *
     * <p>The length byte's high bit signals whether the last byte carries one
     * character with a filler nibble or two characters.
     *
     * @implNote
     * This implementation uses {@code 0x0F} as the filler nibble for the odd
     * trailing character, matching the sentinel the protocol expects.
     *
     * @param input the string to encode
     * @param tag   {@link StanzaTags#NIBBLE_8} or {@link StanzaTags#HEX_8}
     * @throws IOException if the underlying sink rejects a write
     */
    private void writePacked(String input, byte tag) throws IOException {
        var table = tag == NIBBLE_8 ? StanzaPackedFormat.NIBBLE_ENCODE : StanzaPackedFormat.HEX_ENCODE;
        var len = input.length();
        writeByte(tag);
        var byteCount = (len + 1) / 2;
        if ((len & 1) == 1) {
            byteCount |= 128;
        }
        writeByte(byteCount);
        var i = 0;
        for (; i + 1 < len; i += 2) {
            writeByte((table[input.charAt(i)] << 4) | table[input.charAt(i + 1)]);
        }
        if (i < len) {
            writeByte((table[input.charAt(i)] << 4) | 0x0F);
        }
    }

    /**
     * Writes every entry of the supplied attribute map as a key value pair.
     *
     * <p>Iteration follows the map's insertion order so re-encoded stanzas
     * round-trip stably.
     *
     * @param attributes the attributes to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeAttributes(SequencedMap<String, ? extends StanzaAttribute> attributes) throws IOException {
        for (var entry : attributes.entrySet()) {
            writeString(entry.getKey());
            writeAttribute(entry.getValue());
        }
    }

    /**
     * Writes a single attribute value under its variant-specific shape.
     *
     * <p>{@link StanzaAttribute.TextAttribute} goes through the full string
     * compression ladder; {@link StanzaAttribute.BytesAttribute} always uses a
     * length-prefixed binary shape; {@link StanzaAttribute.JidAttribute}
     * dispatches through {@link #writeJid(Jid)}.
     *
     * @param attribute the attribute to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeAttribute(StanzaAttribute attribute) throws IOException {
        switch (attribute) {
            case StanzaAttribute.BytesAttribute(var buffer) -> writeBytesBlob(buffer);
            case StanzaAttribute.JidAttribute(var jid) -> writeJid(jid);
            case StanzaAttribute.TextAttribute(var string) -> writeString(string);
            case StanzaAttribute.StreamAttribute(var value) -> writeStreamBlob(value);
        }
    }

    /**
     * Writes the content slot of a stanza based on its concrete variant.
     *
     * <p>{@link Stanza.EmptyStanza} contributes nothing because the leading size
     * header already encodes the missing content slot; every other variant
     * writes its payload through the appropriate writer.
     *
     * @param content the stanza whose content to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeContent(Stanza content) throws IOException {
        switch (content) {
            case Stanza.EmptyStanza _ -> {}
            case Stanza.BytesStanza(var _, var _, var buffer) -> writeBytesBlob(buffer);
            case Stanza.ContainerStanza(var _, var _, var children) -> writeChildren(children);
            case Stanza.JidStanza(var _, var _, var jid) -> writeJid(jid);
            case Stanza.TextStanza(var _, var _, var text) -> writeString(text);
            case Stanza.StreamStanza(var _, var _, var sized) -> writeStreamBlob(sized);
        }
    }

    /**
     * Writes a list size header followed by the encoding of every child stanza.
     *
     * <p>Recurses through {@link #writeStanzaBody(Stanza)} for each child; the
     * parent's flags byte has already been written by {@link #writeStanza(Stanza)}.
     *
     * @param values the child stanzas to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeChildren(SequencedCollection<Stanza> values) throws IOException {
        writeList(values.size());
        for (var value : values) {
            writeStanzaBody(value);
        }
    }

    /**
     * Writes a binary blob with its length prefix.
     *
     * @param buffer the blob to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeBytesBlob(byte[] buffer) throws IOException {
        var length = buffer.length;
        writeBinary(length);
        writeBytes(buffer, 0, length);
    }

    /**
     * Writes a binary blob streamed from a sized stream with its length prefix.
     *
     * <p>Emits the length prefix from the advertised
     * {@link SizedInputStream#length()}, then drains a fresh stream from
     * {@code content} in fixed-size chunks straight to the sink, so the payload
     * never has to be held in a single byte array. The stream must yield exactly
     * its advertised length; a mismatch is rejected after the fact so a fixed
     * sink that was pre-sized through {@link StanzaSizer#sizeOf(Stanza)} cannot be
     * silently corrupted.
     *
     * @param content the sized stream over the blob
     * @throws IOException           if reading the stream or writing the sink fails,
     *                               or the stream length does not match the advertised length
     * @throws ArithmeticException   if the advertised length exceeds {@link Integer#MAX_VALUE}
     * @throws NullPointerException  if the sized stream yields a {@code null} stream
     */
    private void writeStreamBlob(SizedInputStream content) throws IOException {
        var declared = Math.toIntExact(content.length());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "writing stream blob, declared length {0}", declared);
        writeBinary(declared);
        try (var stream = Objects.requireNonNull(content.openStream(), "content stream supplier yielded null")) {
            var chunk = new byte[8192];
            var total = 0;
            int read;
            while ((read = stream.read(chunk)) != -1) {
                writeBytes(chunk, 0, read);
                total += read;
            }
            if (total != declared) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "stream blob length mismatch, wrote {0} bytes, declared {1}", total, declared);
                throw new IOException("content stream yielded " + total + " bytes but advertised " + declared);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wrote stream blob, {0} bytes", total);
        }
    }

    /**
     * Writes a JID under the wire shape appropriate for its server and device.
     *
     * <p>Messenger JIDs become {@link StanzaTags#JID_FB}; interoperability JIDs
     * become {@link StanzaTags#JID_INTEROP} after splitting a
     * {@code "integrator-user"} compound; device-bearing JIDs become
     * {@link StanzaTags#AD_JID}; everything else becomes {@link StanzaTags#JID_PAIR}
     * with a {@link StanzaTags#LIST_EMPTY} user placeholder when the user
     * component is absent.
     *
     * @param jid the JID to write
     * @throws IOException if the underlying sink rejects a write
     */
    private void writeJid(Jid jid) throws IOException {
        if (jid.hasMessengerServer()) {
            writeByte(JID_FB);
            writeString(jid.user());
            writeShort(jid.device());
            writeString(jid.server().address());
        } else if (jid.hasInteropServer()) {
            writeByte(JID_INTEROP);
            var user = jid.user();
            var dashIndex = user.indexOf('-');
            var integrator = 0;
            var actualUser = user;
            if (dashIndex >= 0) {
                for (var i = 0; i < dashIndex; i++) {
                    integrator = integrator * 10 + (user.charAt(i) - '0');
                }
                actualUser = user.substring(dashIndex + 1);
            }
            writeString(actualUser);
            writeShort(jid.device());
            writeShort(integrator);
        } else if (jid.hasDevice()) {
            writeByte(AD_JID);
            writeByte(getDomainForServer(jid.server()));
            writeByte(jid.device());
            writeString(jid.user());
        } else {
            writeByte(JID_PAIR);
            if (jid.hasUser()) {
                writeString(jid.user());
            } else {
                writeByte(LIST_EMPTY);
            }
            writeString(jid.server().address());
        }
    }

    /**
     * Maps a {@link JidServer} to its multi-device JID domain code.
     *
     * <p>Used only by the {@link StanzaTags#AD_JID} branch of
     * {@link #writeJid(Jid)} to project the server enum into one of the
     * wire-level {@code DOMAIN_*} constants.
     *
     * @param server the server to translate
     * @return one of the {@code DOMAIN_*} constants in {@link StanzaTags}
     */
    private static int getDomainForServer(JidServer server) {
        return switch (server.type()) {
            case LID -> DOMAIN_LID;
            case HOSTED -> DOMAIN_HOSTED;
            case HOSTED_LID -> DOMAIN_HOSTED_LID;
            default -> DOMAIN_WHATSAPP;
        };
    }

    /**
     * Encodes into a caller-supplied {@code byte[]}.
     */
    private static final class ByteArrayWriter extends StanzaWriter {

        /**
         * Holds the destination byte array.
         */
        private final byte[] output;

        /**
         * Holds the current write position into {@link #output}.
         */
        private int position;

        /**
         * Constructs a new {@code byte[]}-backed encoder.
         *
         * @param output the destination byte array
         * @param offset the starting offset
         */
        ByteArrayWriter(byte[] output, int offset) {
            this.output = output;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeByte(int b) {
            output[position++] = (byte) b;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeShort(int v) {
            DataUtils.putShort(output, position, (short) v, ByteOrder.BIG_ENDIAN);
            position += 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeInt(int v) {
            DataUtils.putInt(output, position, v, ByteOrder.BIG_ENDIAN);
            position += 4;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeBytes(byte[] src, int off, int len) {
            System.arraycopy(src, off, output, position, len);
            position += len;
        }
    }

    /**
     * Encodes into a {@link ByteBuffer}, advancing its position as it writes.
     *
     * <p>Forces big-endian order on the buffer at construction so the
     * {@code putShort} and {@code putInt} primitives write multi-byte values in
     * wire order.
     */
    private static final class ByteBufferWriter extends StanzaWriter {

        /**
         * Holds the destination buffer.
         */
        private final ByteBuffer output;

        /**
         * Constructs a new {@link ByteBuffer}-backed encoder.
         *
         * <p>Forces big-endian order on the buffer so multi-byte primitives
         * write in wire order.
         *
         * @param output the destination buffer
         */
        ByteBufferWriter(ByteBuffer output) {
            this.output = output.order(ByteOrder.BIG_ENDIAN);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeByte(int b) {
            output.put((byte) b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeShort(int v) {
            output.putShort((short) v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeInt(int v) {
            output.putInt(v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeBytes(byte[] src, int off, int len) {
            output.put(src, off, len);
        }
    }

    /**
     * Encodes into a {@link MemorySegment} via the FFM API.
     */
    private static final class MemorySegmentWriter extends StanzaWriter {

        /**
         * Holds the big-endian short layout reused across writes.
         */
        private static final ValueLayout.OfShort BE_SHORT =
                ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

        /**
         * Holds the big-endian int layout reused across writes.
         */
        private static final ValueLayout.OfInt BE_INT =
                ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

        /**
         * Holds the destination segment.
         */
        private final MemorySegment output;

        /**
         * Holds the current write offset within {@link #output}.
         */
        private long position;

        /**
         * Constructs a new {@link MemorySegment}-backed encoder.
         *
         * @param output the destination segment
         * @param offset the starting byte offset
         */
        MemorySegmentWriter(MemorySegment output, long offset) {
            this.output = output;
            this.position = offset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeByte(int b) {
            output.set(ValueLayout.JAVA_BYTE, position++, (byte) b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeShort(int v) {
            output.set(BE_SHORT, position, (short) v);
            position += 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeInt(int v) {
            output.set(BE_INT, position, v);
            position += 4;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeBytes(byte[] src, int off, int len) {
            MemorySegment.copy(src, off, output, ValueLayout.JAVA_BYTE, position, len);
            position += len;
        }
    }

    /**
     * Streams encoded tokens directly to an {@link OutputStream} with no
     * intermediate buffer.
     *
     * <p>Each call to a primitive writer translates to one or more
     * {@link OutputStream#write(int)} calls; bulk payloads pass through a
     * single {@link OutputStream#write(byte[], int, int)} call. The encoder
     * allocates nothing per stanza.
     */
    private static final class StreamWriter extends StanzaWriter {

        /**
         * Holds the downstream output stream.
         */
        private final OutputStream output;

        /**
         * Indicates whether this encoder owns {@link #output} and so must close
         * it during {@link #close()}.
         *
         * <p>When {@code false}, the stream is caller-owned and {@link #close()}
         * only flushes it.
         */
        private final boolean owned;

        /**
         * Constructs a new {@link OutputStream}-backed streaming encoder.
         *
         * @param output the downstream stream
         * @param owned  {@code true} when the encoder owns {@code output} and
         *               must close it on {@link #close()}; {@code false} when
         *               the stream is caller-owned and should only be flushed
         */
        StreamWriter(OutputStream output, boolean owned) {
            this.output = output;
            this.owned = owned;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeByte(int b) throws IOException {
            output.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeShort(int v) throws IOException {
            output.write(v >>> 8);
            output.write(v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeInt(int v) throws IOException {
            output.write(v >>> 24);
            output.write(v >>> 16);
            output.write(v >>> 8);
            output.write(v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void writeBytes(byte[] src, int off, int len) throws IOException {
            output.write(src, off, len);
        }

        /**
         * Closes the downstream output stream when the encoder owns
         * it; otherwise only flushes it so the last bytes written
         * through this encoder reach the network without waiting
         * for a later write to amortise the flush.
         *
         * @throws IOException if the downstream flush or close fails
         */
        @Override
        public void close() throws IOException {
            if (owned) {
                output.close();
            } else {
                output.flush();
            }
        }
    }
}
