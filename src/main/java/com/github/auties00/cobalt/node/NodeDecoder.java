package com.github.auties00.cobalt.node;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import static com.github.auties00.cobalt.node.NodeTags.*;
import static com.github.auties00.cobalt.node.NodeTokens.*;

/**
 * A decoder for deserializing WhatsApp protocol nodes from binary ByteBuffer data.
 * <p>
 * This decoder implements the WhatsApp binary protocol specification for deserializing
 * node-based data structures used in WhatsApp communication. It handles various node types,
 * attributes, and children formats including compressed data, JID pairs, binary data,
 * and tokenized strings.
 * <p>
 * The decoder supports:
 * <ul>
 *     <li>Compressed and uncompressed data using DEFLATE algorithm</li>
 *     <li>Multiple binary data formats (8-bit, 20-bit, and 32-bit size prefixes)</li>
 *     <li>Packed hexadecimal and nibble-encoded strings</li>
 *     <li>Dictionary-based token resolution for efficient string encoding</li>
 *     <li>JID parsing for user and device identification</li>
 *     <li>Nested node structures with attributes and child nodes</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * ByteBuffer buffer = ByteBuffer.wrap(encodedData);
 * NodeDecoder decoder = new NodeDecoder(buffer);
 * Node node = decoder.decode();
 * }</pre>
 *
 * @see Node
 * @see NodeAttribute
 * @see NodeEncoder
 * @see NodeTokens
 * @see NodeTags
 */
public final class NodeDecoder implements AutoCloseable {
    /**
     * Alphabet used for decoding nibble-encoded strings (4-bit per character).
     * Contains digits, hyphen, period, and special characters.
     */
    private static final char[] NIBBLE_ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�', '�', '�'};

    /**
     * Alphabet used for decoding hexadecimal-encoded strings (4-bit per character).
     * Contains standard hexadecimal digits 0-9 and A-F.
     */
    private static final char[] HEX_ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Maximum size of the temporary buffer used for decompression operations.
     */
    private static final int MAX_DECOMPRESSION_BUFFER_SIZE = 8192;

    /**
     * The source ByteBuffer containing the encoded node data.
     */
    private final ByteBuffer source;

    /**
     * Indicates whether the data is compressed and requires decompression.
     */
    private final boolean compressionEnabled;

    /**
     * The inflater used for decompressing data when compression is enabled.
     */
    private final Inflater inflater;

    /**
     * Temporary buffer used for decompression operations.
     * Only allocated when compression is enabled.
     * Size is determined based on source ByteBuffer capacity with a maximum of 8192 bytes.
     */
    private final byte[] decompressionBuffer;

    /**
     * Current position in the decompression buffer for reading.
     */
    private int bufferPosition;

    /**
     * Number of valid bytes available in the decompression buffer.
     */
    private int bufferLimit;

    /**
     * Constructs a new NodeDecoder with the provided ByteBuffer.
     * <p>
     * The constructor automatically detects whether the data is compressed by reading
     * the first byte's compression flag (bit 2). If compression is detected, an
     * inflater and temporary decompression buffer are initialized. The decompression
     * buffer size is calculated as the minimum of 8192 bytes and the maximum possible
     * expanded size based on the source ByteBuffer's remaining capacity.
     *
     * @param source the ByteBuffer containing the encoded node data
     */
    public NodeDecoder(ByteBuffer source) {
        this.source = source;
        var flags = source.get() & 0xFF;
        this.compressionEnabled = (flags & 2) != 0;
        if (compressionEnabled) {
            this.inflater = new Inflater();
            this.decompressionBuffer = new byte[MAX_DECOMPRESSION_BUFFER_SIZE];
            this.bufferPosition = 0;
            this.bufferLimit = 0;
        } else {
            this.inflater = null;
            this.decompressionBuffer = null;
        }
    }

    /**
     * Decodes a node from the ByteBuffer.
     * <p>
     * This method reads from either the source ByteBuffer directly (if compression
     * is disabled) or from the decompression buffer (if compression is enabled).
     *
     * @return the decoded {@link Node} object representing the node structure
     * @throws IOException if an I/O error occurs while reading or decompressing data
     */
    public Node decode() throws IOException {
        return readNode();
    }

    /**
     * Checks if there is more data available to be processed.
     * <p>
     * If compression is disabled, this checks if the source ByteBuffer has remaining bytes.
     * If compression is enabled, this checks if there are bytes in the decompression buffer
     * or if the inflater has not finished processing all data.
     *
     * @return true if more data is available to read, false otherwise
     */
    public boolean hasData() {
        if (!compressionEnabled) {
            return source.hasRemaining();
        }else {
            return bufferPosition < bufferLimit
                   || !inflater.finished()
                   || source.hasRemaining();
        }
    }

    /**
     * Reads a single byte from the appropriate source.
     * <p>
     * If compression is disabled, reads directly from the source ByteBuffer.
     * If compression is enabled, reads from the decompression buffer, filling it
     * from the compressed source as needed.
     *
     * @return the next byte value (0-255)
     * @throws IOException if an I/O error occurs or end of data is reached
     */
    private int read() throws IOException {
        if (!compressionEnabled) {
            if (!source.hasRemaining()) {
                throw new IOException("Unexpected end of data");
            }
            return source.get() & 0xFF;
        }

        if (bufferPosition >= bufferLimit) {
            fillDecompressionBuffer();
        }

        if (bufferPosition >= bufferLimit) {
            throw new IOException("Unexpected end of decompressed data");
        }

        return decompressionBuffer[bufferPosition++] & 0xFF;
    }

    /**
     * Reads the specified number of bytes into a new byte array.
     * <p>
     * If compression is disabled, reads directly from the source ByteBuffer.
     * If compression is enabled, reads from the decompression buffer, filling it
     * from the compressed source as needed.
     *
     * @param length the number of bytes to read
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs or insufficient data is available
     */
    private byte[] readBytes(int length) throws IOException {
        var result = new byte[length];

        if (!compressionEnabled) {
            if (source.remaining() < length) {
                throw new IOException("Insufficient data available");
            }
            source.get(result);
            return result;
        }

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
     * Fills the decompression buffer by inflating data from the source ByteBuffer.
     * <p>
     * This method feeds compressed data from the source to the inflater and
     * decompresses it into the temporary buffer.
     *
     * @throws IOException if a decompression error occurs
     */
    private void fillDecompressionBuffer() throws IOException {
        try {
            if (inflater.needsInput() && source.hasRemaining()) {
                var available = source.remaining();
                var input = new byte[Math.min(available, decompressionBuffer.length)];
                source.get(input);
                inflater.setInput(input);
            }

            bufferPosition = 0;
            bufferLimit = inflater.inflate(decompressionBuffer);
        } catch (DataFormatException e) {
            throw new IOException("Decompression error", e);
        }
    }

    /**
     * Reads and decodes a complete node from the data source.
     * <p>
     * The node structure consists of:
     * <ul>
     *     <li>Size indicator (determines number of attributes and children presence)</li>
     *     <li>Description string (optional)</li>
     *     <li>Attributes as key-value pairs (each pair consumes 2 size units)</li>
     *     <li>Content (present if size is odd after accounting for attributes)</li>
     * </ul>
     *
     * @return the decoded {@link Node} which may be an EmptyNode, TextNode, BufferNode,
     *         JidNode, ContainerNode, or null
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private Node readNode() throws IOException {
        var size = readNodeSize();
        if(size == 0) {
            return Node.empty();
        }

        var description = readString();
        var attrs = readAttributes(size - 1);

        if((size & 1) == 1) {
            return new Node.EmptyNode(description, attrs);
        }

        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> new Node.EmptyNode(description, attrs);
            case AD_JID -> new Node.JidNode(description, attrs, readAdJid());
            case LIST_8 -> new Node.ContainerNode(description, attrs, readList8());
            case LIST_16 -> new Node.ContainerNode(description, attrs, readList16());
            case JID_PAIR -> new Node.JidNode(description, attrs, readJidPair());
            case HEX_8 -> new Node.TextNode(description, attrs, readPacked(HEX_ALPHABET));
            case BINARY_8 -> new Node.BytesContent(description, attrs, readBinary8());
            case BINARY_20 -> new Node.BytesContent(description, attrs, readBinary20());
            case BINARY_32 -> new Node.BytesContent(description, attrs, readBinary32());
            case NIBBLE_8 -> new Node.TextNode(description, attrs, readPacked(NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new Node.TextNode(description, attrs, readDictionaryToken(DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new Node.TextNode(description, attrs, readDictionaryToken(DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new Node.TextNode(description, attrs, readDictionaryToken(DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new Node.TextNode(description, attrs, readDictionaryToken(DICTIONARY_3_TOKENS));
            default -> new Node.TextNode(description, attrs, readSingleByteToken(tag));
        };
    }

    /**
     * Reads the size indicator that determines the node structure.
     * <p>
     * Supports two size formats:
     * <ul>
     *     <li>LIST_8: 8-bit size (0-255)</li>
     *     <li>LIST_16: 16-bit size (0-65535)</li>
     * </ul>
     *
     * @return the size value indicating number of elements in the node structure
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an unexpected size token is encountered
     */
    private int readNodeSize() throws IOException {
        var token = (byte) read();
        return switch (token) {
            case LIST_8 -> read() & 0xFF;
            case LIST_16 -> (read() << 8) | read();
            default -> throw new IllegalStateException("Unexpected value: " + token);
        };
    }

    /**
     * Reads and decodes a string based on its encoding tag.
     * <p>
     * Supports multiple string encoding formats including packed hexadecimal,
     * nibble encoding, binary data, dictionary tokens, and single-byte tokens.
     *
     * @return the decoded string, or null if LIST_EMPTY tag is encountered
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private String readString() throws IOException {
        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case HEX_8 -> readPacked(HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8());
            case BINARY_20 -> new String(readBinary20());
            case BINARY_32 -> new String(readBinary32());
            case DICTIONARY_0 -> readDictionaryToken(DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    /**
     * Reads binary data with an 8-bit size prefix (up to 255 bytes).
     *
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private byte[] readBinary8() throws IOException {
        var size = read() & 0xFF;
        return readBytes(size);
    }

    /**
     * Reads binary data with a 20-bit size prefix (up to 1,048,575 bytes).
     * <p>
     * Size is encoded in big-endian format across 3 bytes.
     *
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private byte[] readBinary20() throws IOException {
        var size = (read() << 16)
                   | (read() << 8)
                   | read();
        return readBytes(size);
    }

    /**
     * Reads binary data with a 32-bit size prefix (up to 2,147,483,647 bytes).
     * <p>
     * Size is encoded in big-endian format across 4 bytes.
     *
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private byte[] readBinary32() throws IOException {
        var size = (read() << 24)
                   | (read() << 16)
                   | (read() << 8)
                   | read();
        return readBytes(size);
    }

    /**
     * Reads a token from a specified dictionary using an 8-bit index.
     * <p>
     * Dictionaries provide efficient string encoding by mapping frequently used
     * strings to single-byte indices.
     *
     * @param dictionary the token dictionary to use for lookup
     * @return the string value associated with the read index
     * @throws IOException if an I/O error occurs
     */
    private String readDictionaryToken(NodeTokens dictionary) throws IOException {
        var index = read() & 0xFF;
        return dictionary.get(index);
    }

    /**
     * Reads a single-byte token from the global single-byte token dictionary.
     * <p>
     * Used for very common strings that can be represented by a single byte.
     *
     * @param tag the byte tag representing the token index
     * @return the string value associated with the token
     */
    private String readSingleByteToken(byte tag) {
        var index = tag & 0xFF;
        return SINGLE_BYTE_TOKENS.get(index);
    }

    /**
     * Reads a sequenced map of attributes.
     * <p>
     * Each attribute consists of a key-value pair, consuming 2 size units.
     * The order of attributes is preserved in the returned map.
     *
     * @param size the number of remaining size units (must be even for complete attributes)
     * @return a sequenced map of attribute keys to {@link NodeAttribute} values
     * @throws IOException if an I/O error occurs during reading
     */
    private SequencedMap<String, NodeAttribute> readAttributes(int size) throws IOException {
        var attributes = new LinkedHashMap<String, NodeAttribute>();
        while (size >= 2) {
            var key = readString();
            var value = readAttribute();
            attributes.put(key, value);
            size -= 2;
        }
        return attributes;
    }

    /**
     * Reads and decodes a single attribute value.
     * <p>
     * Attributes can be text, bytes, or JID values, encoded using various formats
     * similar to node children encoding.
     *
     * @return a {@link NodeAttribute} object representing the attribute value, or null if empty
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if unexpected list tags (LIST_8 or LIST_16) are encountered
     */
    private NodeAttribute readAttribute() throws IOException {
        var tag = (byte) read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case AD_JID -> new NodeAttribute.JidAttribute(readAdJid());
            case LIST_8 -> throw new IllegalStateException("Unexpected LIST_8 tag");
            case LIST_16 -> throw new IllegalStateException("Unexpected LIST_16 tag");
            case JID_PAIR -> new NodeAttribute.JidAttribute(readJidPair());
            case HEX_8 -> new NodeAttribute.TextAttribute(readPacked(HEX_ALPHABET));
            case BINARY_8 -> new NodeAttribute.BytesAttribute(readBinary8());
            case BINARY_20 -> new NodeAttribute.BytesAttribute(readBinary20());
            case BINARY_32 -> new NodeAttribute.BytesAttribute(readBinary32());
            case NIBBLE_8 -> new NodeAttribute.TextAttribute(readPacked(NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new NodeAttribute.TextAttribute(readDictionaryToken(DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new NodeAttribute.TextAttribute(readDictionaryToken(DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new NodeAttribute.TextAttribute(readDictionaryToken(DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new NodeAttribute.TextAttribute(readDictionaryToken(DICTIONARY_3_TOKENS));
            default -> new NodeAttribute.TextAttribute(readSingleByteToken(tag));
        };
    }

    /**
     * Reads a sequence of nodes with an 8-bit length prefix (up to 255 nodes).
     *
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs
     */
    private SequencedCollection<Node> readList8() throws IOException {
        var length = read() & 0xFF;
        return readList(length);
    }

    /**
     * Reads a sequence of nodes with a 16-bit length prefix (up to 65,535 nodes).
     * <p>
     * Length is encoded in big-endian format across 2 bytes.
     *
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs
     */
    private SequencedCollection<Node> readList16() throws IOException {
        var length = (read() << 8)
                     | read();
        return readList(length);
    }

    /**
     * Reads a sequence of nodes with the specified size.
     * <p>
     * Each node in the list is decoded sequentially.
     *
     * @param size the number of nodes to read
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private SequencedCollection<Node> readList(int size) throws IOException {
        var results = new ArrayList<Node>(size);
        for (var index = 0; index < size; index++) {
            var node = readNode();
            results.add(node);
        }
        return results;
    }

    /**
     * Reads a packed string encoded using the specified alphabet.
     * <p>
     * Packed encoding stores two characters per byte, with each character represented
     * by 4 bits (nibble). The first byte contains metadata:
     * <ul>
     *     <li>Bit 7: Start offset (0 or 1)</li>
     *     <li>Bits 0-6: End position</li>
     * </ul>
     * If the start offset is 1, the last character is stored in the high nibble
     * of an additional byte.
     *
     * @param alphabet the character alphabet to use for decoding nibbles (must have 16 elements)
     * @return the decoded string
     * @throws IOException if an I/O error occurs
     */
    private String readPacked(char[] alphabet) throws IOException {
        var token = read() & 0xFF;
        var start = token >>> 7;
        var end = token & 127;
        var string = new char[2 * end - start];
        for(var index = 0; index < string.length - 1; index += 2) {
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
     * Reads a JID pair consisting of a user and server component.
     * <p>
     * A JID pair represents a WhatsApp user or group identifier. If the user
     * component is null, a server-only JID is created.
     *
     * @return a {@link Jid} object representing the user or group
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the server component is null (malformed pair)
     */
    private Jid readJidPair() throws IOException {
        var user = readString();
        var server = JidServer.of(Objects.requireNonNull(readString(), "Malformed value pair: no server"));
        return user == null ? Jid.of(server) : Jid.of(user, server);
    }

    /**
     * Reads a JID which includes agent and device identifiers.
     * <p>
     * AD JIDs are used for multi-device WhatsApp accounts and contain:
     * <ul>
     *     <li>Agent ID (8-bit)</li>
     *     <li>Device ID (8-bit)</li>
     *     <li>User identifier string</li>
     * </ul>
     * The resulting JID uses the user server type with device and agent metadata.
     *
     * @return a {@link Jid} object representing the device-specific user identifier
     * @throws IOException if an I/O error occurs
     */
    private Jid readAdJid() throws IOException {
        var agent = read() & 0xFF;
        var device = read() & 0xFF;
        var user = readString();
        return Jid.of(user, JidServer.user(), device, agent);
    }

    @Override
    public void close() {
        if(inflater != null) {
            inflater.close();
        }
    }
}