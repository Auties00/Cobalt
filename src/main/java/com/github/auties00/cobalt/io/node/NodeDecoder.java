
package com.github.auties00.cobalt.io.node;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.InflaterInputStream;

import static com.github.auties00.cobalt.io.node.NodeTags.*;
import static com.github.auties00.cobalt.io.node.NodeTokens.*;

/**
 * A utility class for decoding WhatsApp protocol nodes from binary input streams.
 * <p>
 * This decoder implements the WhatsApp binary protocol specification for deserializing
 * node-based data structures used in WhatsApp communication. It handles various node types,
 * attributes, and children formats including compressed streams, JID pairs, binary data,
 * and tokenized strings.
 * <p>
 * The decoder supports:
 * <ul>
 *     <li>Compressed and uncompressed streams using DEFLATE algorithm</li>
 *     <li>Multiple binary data formats (8-bit, 20-bit, and 32-bit size prefixes)</li>
 *     <li>Packed hexadecimal and nibble-encoded strings</li>
 *     <li>Dictionary-based token resolution for efficient string encoding</li>
 *     <li>JID parsing for user and device identification</li>
 *     <li>Nested node structures with attributes and child nodes</li>
 * </ul>
 * <p>
 * This class cannot be instantiated as it serves as a utility class with static methods only.
 *
 * @see Node
 * @see NodeAttribute
 * @see NodeEncoder
 * @see NodeTokens
 * @see NodeTags
 */
public final class NodeDecoder {
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
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always thrown when instantiation is attempted
     */
    private NodeDecoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Decodes a node from the provided input stream.
     * <p>
     * This method automatically detects whether the stream is compressed by reading
     * the first byte's compression flag (bit 2). If compression is detected, the stream
     * is wrapped in an {@link InflaterInputStream} before decoding.
     *
     * @param stream the input stream containing the encoded node data
     * @return the decoded {@link Node} object representing the node structure
     * @throws IOException if an I/O error occurs while reading from the stream
     */
    public static Node decode(InputStream stream) throws IOException {
        if((stream.read() & 2) == 0) {
            return readNode(stream);
        }else {
            return readNode(new InflaterInputStream(stream));
        }
    }

    /**
     * Reads and decodes a complete node from the input stream.
     * <p>
     * The node structure consists of:
     * <ul>
     *     <li>Size indicator (determines number of attributes and children presence)</li>
     *     <li>Description string (optional)</li>
     *     <li>Attributes as key-value pairs (each pair consumes 2 size units)</li>
     *     <li>Content (present if size is odd after accounting for attributes)</li>
     * </ul>
     *
     * @param inputStream the input stream to read from
     * @return the decoded {@link Node} which may be an EmptyNode, TextNode, BufferNode,
     *         JidNode, ContainerNode, or null
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private static Node readNode(InputStream inputStream) throws IOException {
        // Read the size of the node
        var size = readNodeSize(inputStream);

        // the description takes up one length unit
        var description = "";
        if(size > 0) {
            size--;
            description = readString(inputStream);
        }

        // the attributes take up two length units for each entry(key and value pair)
        var attrs = readAttributes(inputStream, size);

        // if the length, including the attributes but not the description, is odd then there is children
        if((size & 1) == 1) {
            return new Node.EmptyNode(description, attrs);
        }

        // Read the children of the node
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> new Node.EmptyNode(description, attrs);
            case AD_JID -> new Node.JidNode(description, attrs, readAdJid(inputStream));
            case LIST_8 -> new Node.ContainerNode(description, attrs, readList8(inputStream));
            case LIST_16 -> new Node.ContainerNode(description, attrs, readList16(inputStream));
            case JID_PAIR -> new Node.JidNode(description, attrs, readJidPair(inputStream));
            case HEX_8 -> new Node.TextNode(description, attrs, readPacked(inputStream, HEX_ALPHABET));
            case BINARY_8 -> new Node.BufferNode(description, attrs, readBinary8(inputStream));
            case BINARY_20 -> new Node.BufferNode(description, attrs, readBinary20(inputStream));
            case BINARY_32 -> new Node.BufferNode(description, attrs, readBinary32(inputStream));
            case NIBBLE_8 -> new Node.TextNode(description, attrs, readPacked(inputStream, NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new Node.TextNode(description, attrs, readDictionaryToken(inputStream, DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new Node.TextNode(description, attrs, readDictionaryToken(inputStream, DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new Node.TextNode(description, attrs, readDictionaryToken(inputStream, DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new Node.TextNode(description, attrs, readDictionaryToken(inputStream, DICTIONARY_3_TOKENS));
            default -> new Node.TextNode(description, attrs, readSingleByteToken(tag));
        };
    }

    /**
     * Reads the size indicator from the input stream that determines the node structure.
     * <p>
     * Supports two size formats:
     * <ul>
     *     <li>LIST_8: 8-bit size (0-255)</li>
     *     <li>LIST_16: 16-bit size (0-65535)</li>
     * </ul>
     *
     * @param inputStream the input stream to read from
     * @return the size value indicating number of elements in the node structure
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an unexpected size token is encountered
     */
    private static int readNodeSize(InputStream inputStream) throws IOException {
        var token = (byte) inputStream.read();
        return switch (token) {
            case LIST_8 -> inputStream.read() & 0xFF;
            case LIST_16 -> (inputStream.read() << 8) | inputStream.read();
            default -> throw new IllegalStateException("Unexpected value: " + token);
        };
    }

    /**
     * Reads and decodes a string from the input stream based on its encoding tag.
     * <p>
     * Supports multiple string encoding formats including packed hexadecimal,
     * nibble encoding, binary data, dictionary tokens, and single-byte tokens.
     *
     * @param inputStream the input stream to read from
     * @return the decoded string, or null if LIST_EMPTY tag is encountered
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private static String readString(InputStream inputStream) throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case HEX_8 -> readPacked(inputStream, HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(inputStream, NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8(inputStream));
            case BINARY_20 -> new String(readBinary20(inputStream));
            case BINARY_32 -> new String(readBinary32(inputStream));
            case DICTIONARY_0 -> readDictionaryToken(inputStream, DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(inputStream, DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(inputStream, DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(inputStream, DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    /**
     * Reads binary data with an 8-bit size prefix (up to 255 bytes).
     *
     * @param inputStream the input stream to read from
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private static byte[] readBinary8(InputStream inputStream) throws IOException {
        var size = inputStream.read() & 0xFF;
        return inputStream.readNBytes(size);
    }

    /**
     * Reads binary data with a 20-bit size prefix (up to 1,048,575 bytes).
     * <p>
     * Size is encoded in big-endian format across 3 bytes.
     *
     * @param inputStream the input stream to read from
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private static byte[] readBinary20(InputStream inputStream) throws IOException {
        var size = (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    /**
     * Reads binary data with a 32-bit size prefix (up to 2,147,483,647 bytes).
     * <p>
     * Size is encoded in big-endian format across 4 bytes.
     *
     * @param inputStream the input stream to read from
     * @return a byte array containing the read data
     * @throws IOException if an I/O error occurs
     */
    private static byte[] readBinary32(InputStream inputStream) throws IOException {
        var size = (inputStream.read() << 24)
                | (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    /**
     * Reads a token from a specified dictionary using an 8-bit index.
     * <p>
     * Dictionaries provide efficient string encoding by mapping frequently used
     * strings to single-byte indices.
     *
     * @param inputStream the input stream to read from
     * @param dictionary the token dictionary to use for lookup
     * @return the string value associated with the read index
     * @throws IOException if an I/O error occurs
     */
    private static String readDictionaryToken(InputStream inputStream, NodeTokens dictionary) throws IOException {
        var index = inputStream.read() & 0xFF;
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
    private static String readSingleByteToken(byte tag) {
        var index = tag & 0xFF;
        return SINGLE_BYTE_TOKENS.get(index);
    }

    /**
     * Reads a sequenced map of attributes from the input stream.
     * <p>
     * Each attribute consists of a key-value pair, consuming 2 size units.
     * The order of attributes is preserved in the returned map.
     *
     * @param inputStream the input stream to read from
     * @param size the number of remaining size units (must be even for complete attributes)
     * @return a sequenced map of attribute keys to {@link NodeAttribute} values
     * @throws IOException if an I/O error occurs during reading
     */
    private static SequencedMap<String, NodeAttribute> readAttributes(InputStream inputStream, int size) throws IOException {
        var attributes = new LinkedHashMap<String, NodeAttribute>();
        while (size >= 2) {
            var key = readString(inputStream);
            var value = readAttribute(inputStream);
            attributes.put(key, value);
            size -= 2;
        }
        return attributes;
    }

    /**
     * Reads and decodes a single attribute value from the input stream.
     * <p>
     * Attributes can be text, bytes, or JID values, encoded using various formats
     * similar to node children encoding.
     *
     * @param inputStream the input stream to read from
     * @return a {@link NodeAttribute} object representing the attribute value, or null if empty
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if unexpected list tags (LIST_8 or LIST_16) are encountered
     */
    private static NodeAttribute readAttribute(InputStream inputStream) throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case AD_JID -> new NodeAttribute.JidAttribute(readAdJid(inputStream));
            case LIST_8 -> throw new IllegalStateException("Unexpected LIST_8 tag");
            case LIST_16 -> throw new IllegalStateException("Unexpected LIST_16 tag");
            case JID_PAIR -> new NodeAttribute.JidAttribute(readJidPair(inputStream));
            case HEX_8 -> new NodeAttribute.TextAttribute(readPacked(inputStream, HEX_ALPHABET));
            case BINARY_8 -> new NodeAttribute.BytesAttribute(readBinary8(inputStream));
            case BINARY_20 -> new NodeAttribute.BytesAttribute(readBinary20(inputStream));
            case BINARY_32 -> new NodeAttribute.BytesAttribute(readBinary32(inputStream));
            case NIBBLE_8 -> new NodeAttribute.TextAttribute(readPacked(inputStream, NIBBLE_ALPHABET));
            case DICTIONARY_0 -> new NodeAttribute.TextAttribute(readDictionaryToken(inputStream, DICTIONARY_0_TOKENS));
            case DICTIONARY_1 -> new NodeAttribute.TextAttribute(readDictionaryToken(inputStream, DICTIONARY_1_TOKENS));
            case DICTIONARY_2 -> new NodeAttribute.TextAttribute(readDictionaryToken(inputStream, DICTIONARY_2_TOKENS));
            case DICTIONARY_3 -> new NodeAttribute.TextAttribute(readDictionaryToken(inputStream, DICTIONARY_3_TOKENS));
            default -> new NodeAttribute.TextAttribute(readSingleByteToken(tag));
        };
    }

    /**
     * Reads a sequence of nodes with an 8-bit length prefix (up to 255 nodes).
     *
     * @param inputStream the input stream to read from
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs
     */
    private static SequencedCollection<Node> readList8(InputStream inputStream) throws IOException {
        var length = inputStream.read() & 0xFF;
        return readList(inputStream, length);
    }

    /**
     * Reads a sequence of nodes with a 16-bit length prefix (up to 65,535 nodes).
     * <p>
     * Length is encoded in big-endian format across 2 bytes.
     *
     * @param inputStream the input stream to read from
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs
     */
    private static SequencedCollection<Node> readList16(InputStream inputStream) throws IOException {
        var length = (inputStream.read() << 8)
                | inputStream.read();
        return readList(inputStream, length);
    }

    /**
     * Reads a sequence of nodes with the specified size.
     * <p>
     * Each node in the list is decoded sequentially.
     *
     * @param inputStream the input stream to read from
     * @param size the number of nodes to read
     * @return a sequence of decoded {@link Node} objects
     * @throws IOException if an I/O error occurs during reading or decoding
     */
    private static SequencedCollection<Node> readList(InputStream inputStream, int size) throws IOException {
        var results = new ArrayList<Node>(size);
        for (int index = 0; index < size; index++) {
            var node = readNode(inputStream);
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
     * @param inputStream the input stream to read from
     * @param alphabet the character alphabet to use for decoding nibbles (must have 16 elements)
     * @return the decoded string
     * @throws IOException if an I/O error occurs
     */
    private static String readPacked(InputStream inputStream, char[] alphabet) throws IOException {
        var token = inputStream.read() & 0xFF;
        var start = token >>> 7;
        var end = token & 127;
        var string = new char[2 * end - start];
        for(var index = 0; index < string.length - 1; index += 2) {
            token = inputStream.read() & 0xFF;
            string[index] = alphabet[token >>> 4];
            string[index + 1] = alphabet[15 & token];
        }
        if (start != 0) {
            token = inputStream.read() & 0xFF;
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
     * @param inputStream the input stream to read from
     * @return a {@link Jid} object representing the user or group
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the server component is null (malformed pair)
     */
    private static Jid readJidPair(InputStream inputStream) throws IOException {
        var user = readString(inputStream);
        var server = JidServer.of(Objects.requireNonNull(readString(inputStream), "Malformed value pair: no server"));
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
     * @param inputStream the input stream to read from
     * @return a {@link Jid} object representing the device-specific user identifier
     * @throws IOException if an I/O error occurs
     */
    private static Jid readAdJid(InputStream inputStream) throws IOException {
        var agent = inputStream.read() & 0xFF;
        var device = inputStream.read() & 0xFF;
        var user = readString(inputStream);
        return Jid.of(user, JidServer.user(), device, agent);
    }
}