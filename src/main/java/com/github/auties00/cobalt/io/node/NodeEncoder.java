package com.github.auties00.cobalt.io.node;

import com.github.auties00.cobalt.model.core.node.Node;
import com.github.auties00.cobalt.model.core.node.NodeAttribute;
import com.github.auties00.cobalt.model.proto.jid.Jid;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.SequencedCollection;
import java.util.SequencedMap;

import static com.github.auties00.cobalt.io.node.NodeTags.*;
import static com.github.auties00.cobalt.io.node.NodeTokens.*;

/**
 * A utility class responsible for encoding {@link Node} objects into binary format
 * for transmission in the WhatsApp protocol.
 * <p>
 * This encoder implements WhatsApp's proprietary binary protocol that uses token-based
 * compression to reduce message size. The encoding process involves:
 * <ul>
 *   <li>Converting strings to dictionary tokens when possible (using single-byte or multi-dictionary lookup)</li>
 *   <li>Encoding binary data with length prefixes</li>
 *   <li>Efficiently serializing node trees with attributes and children</li>
 *   <li>Supporting various children types: text, binary buffers, JIDs, streams, and child nodes</li>
 * </ul>
 * <p>
 * The encoding format is optimized for small message sizes and includes:
 * <ul>
 *   <li>Token dictionaries (SINGLE_BYTE_TOKENS, DICTIONARY_0-3_TOKENS) for common strings</li>
 *   <li>Variable-length integer encoding (8-bit, 20-bit, 32-bit)</li>
 *   <li>List size encoding (8-bit or 16-bit)</li>
 *   <li>Special encoding for WhatsApp JIDs</li>
 * </ul>
 * <p>
 * This class is thread-safe as all methods are static and operate on provided parameters
 * without shared mutable state.
 *
 * @see Node
 * @see NodeDecoder
 * @see NodeTokens
 * @see NodeTags
 */
public final class NodeEncoder {
    /**
     * Maximum value for unsigned byte (2^8).
     */
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;

    /**
     * Maximum value for unsigned short (2^16).
     */
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;

    /**
     * Maximum value for 20-bit integer (2^20).
     */
    private static final int INT_20_MAX_VALUE = 1048576;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always, as this class should not be instantiated
     */
    private NodeEncoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Calculates the total size in bytes required to encode the given node.
     * <p>
     * This includes the message header (1 byte) and the full encoded length of the node
     * including its description, attributes, and children.
     *
     * @param node the node to calculate the size for
     * @return the total number of bytes required to encode the node
     * @throws IllegalArgumentException if the node is too large to encode
     */
    public static int sizeOf(Node node) {
        return 1 + nodeLength(node);
    }

    /**
     * Calculates the length of a node's encoding, excluding the message header.
     *
     * @param input the node to calculate the length for
     * @return the length in bytes
     */
    private static int nodeLength(Node input){
        return listLength(input.size())
                + stringLength(input.description())
                + attributesLength(input.attributes())
                + contentLength(input);
    }

    /**
     * Calculates the number of bytes required to encode a list size.
     * <p>
     * Uses 8-bit encoding (LIST_8) for sizes less than 256, and 16-bit encoding (LIST_16)
     * for sizes less than 65536.
     *
     * @param size the size of the list
     * @return the number of bytes required (2 or 3)
     * @throws IllegalArgumentException if the size exceeds the maximum supported value
     */
    private static int listLength(int size) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            return 2;
        }else if (size < UNSIGNED_SHORT_MAX_VALUE) {
            return 3;
        }else {
            throw new IllegalArgumentException("Cannot calculate list length: overflow");
        }
    }

    /**
     * Calculates the number of bytes required to encode a string.
     * <p>
     * The encoding strategy prioritizes efficiency:
     * <ol>
     *   <li>Empty strings use 2 bytes (BINARY_8 + LIST_EMPTY)</li>
     *   <li>Strings in SINGLE_BYTE_TOKENS dictionary use 1 byte</li>
     *   <li>Strings in DICTIONARY_0-3 use 2 bytes (dictionary tag + index)</li>
     *   <li>Other strings are UTF-8 encoded with a length prefix</li>
     * </ol>
     *
     * @param input the string to calculate the encoding length for
     * @return the number of bytes required to encode the string
     */
    private static int stringLength(String input){
        if (input.isEmpty()) {
            return 2;
        }

        var singleByteTokenIndex = SINGLE_BYTE_TOKENS.indexOf(input);
        if (singleByteTokenIndex != -1) {
            return 1;
        }

        var dictionary0TokenIndex = DICTIONARY_0_TOKENS.indexOf(input);
        if (dictionary0TokenIndex != -1) {
            return 2;
        }

        var dictionary1TokenIndex = DICTIONARY_1_TOKENS.indexOf(input);
        if (dictionary1TokenIndex != -1) {
            return 2;
        }

        var dictionary2TokenIndex = DICTIONARY_2_TOKENS.indexOf(input);
        if (dictionary2TokenIndex != -1) {
            return 2;
        }

        var dictionary3TokenIndex = DICTIONARY_3_TOKENS.indexOf(input);
        if (dictionary3TokenIndex != -1) {
            return 2;
        }

        var length = calculateUtf8Length(input);
        return calculateLength(length);
    }

    /**
     * Calculates the number of bytes required to encode a binary length prefix.
     *
     * @param input the length value to encode
     * @return the number of bytes required (2, 4, or 5)
     */
    private static int binaryLength(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            return 2;
        }else if (input < INT_20_MAX_VALUE) {
            return 4;
        }else {
            return 5;
        }
    }

    /**
     * Calculates the total number of bytes required to encode a map of node attributes.
     *
     * @param attributes the attributes to encode
     * @return the total number of bytes required
     */
    private static int attributesLength(SequencedMap<String, ? extends NodeAttribute> attributes) {
        var result = 0;
        for (var entry : attributes.entrySet()) {
            result += stringLength(entry.getKey()) + attributeLength(entry.getValue());
        }
        return result;
    }

    /**
     * Calculates the number of bytes required to encode a single node attribute value.
     *
     * @param attribute the attribute to encode
     * @return the number of bytes required
     */
    private static int attributeLength(NodeAttribute attribute){
        return switch (attribute) {
            case NodeAttribute.BytesAttribute(var bytes) -> bytesLength(bytes);
            case NodeAttribute.TextAttribute(var literal) -> stringLength(literal);
            case NodeAttribute.JidAttribute(var jid) -> jidLength(jid);
        };
    }

    /**
     * Calculates the total number of bytes required to encode a collection of child nodes.
     *
     * @param values the child nodes to encode
     * @return the total number of bytes required
     */
    private static int childrenLength(SequencedCollection<Node> values) {
        var length = listLength(values.size());
        for(var value : values) {
            length += nodeLength(value);
        }
        return length;
    }

    /**
     * Calculates the number of bytes required to encode a node's children.
     *
     * @param node the node whose children to calculate
     * @return the number of bytes required
     */
    private static int contentLength(Node node){
        return switch (node) {
            case Node.BytesContent(var _, var _, var bytes) -> bytesLength(bytes);
            case Node.ContainerNode(var _, var _, var children) -> childrenLength(children);
            case Node.EmptyNode _ -> 0;
            case Node.JidNode(var _, var _, var jid) -> jidLength(jid);
            case Node.StreamNode(var _, var _, var _, var streamLength) -> calculateLength(streamLength);
            case Node.TextNode(var _, var _, var text) -> stringLength(text);
        };
    }

    /**
     * Calculates the number of bytes required to encode an array of bytes.
     *
     * @param bytes the array of bytes to encode
     * @return the number of bytes required (length prefix + data)
     */
    private static int bytesLength(byte[] bytes){
        return calculateLength(bytes.length);
    }

    /**
     * Calculates the number of bytes required to encode a WhatsApp JID.
     * <p>
     * JIDs can be encoded in two ways:
     * <ul>
     *   <li>AD_JID format: for JIDs with agent or device information (3 bytes + user string)</li>
     *   <li>JID_PAIR format: standard format with user and server (2 bytes + user string + 1 or server string)</li>
     * </ul>
     *
     * @param jid the JID to encode
     * @return the number of bytes required
     */
    private static int jidLength(Jid jid){
        if (jid.hasAgent() || jid.hasDevice()) {
            return 3 + stringLength(jid.user());
        }else {
            return 2 + (jid.hasUser() ? stringLength(jid.user()) : 1);
        }
    }

    /**
     * Calculates the total number of bytes required to encode data with a length prefix.
     *
     * @param length the length of the data
     * @return the number of bytes required (prefix + data)
     */
    private static int calculateLength(int length) {
        return binaryLength(length) + length;
    }

    /**
     * Calculates the total number of bytes required to encode a UTF-8 string.
     *
     * @param input the UTF-8 string to calculate the length for
     * @return the number of bytes required
     */
    private static int calculateUtf8Length(String input) {
        var length = 0;
        if(input == null) {
            return length;
        }

        var len = input.length();
        for (var i = 0; i < len; i++) {
            var ch = input.charAt(i);
            if (ch <= 0x7F) {
                length++;
            } else if (ch <= 0x7FF) {
                length += 2;
            } else if (Character.isHighSurrogate(ch)) {
                length += 4;
                i++;
            } else {
                length += 3;
            }
        }
        return length;
    }

    /**
     * Encodes a node into the provided byte array at the specified offset.
     *
     * @param node the node to encode
     * @param output the output byte array
     * @param offset the offset in the output array where encoding should start
     */
    public static void encode(Node node, byte[] output, int offset) {
        writeMessage(node, output, offset);
    }

    /**
     * Encodes a node into a new byte array.
     * <p>
     * This method first calculates the required size, allocates a byte array,
     * and then encodes the node into it.
     *
     * @param node the node to encode
     * @return a byte array containing the encoded node
     * @throws InternalError if the encoding position doesn't match the expected length
     */
    public static byte[] encode(Node node) {
        var length = sizeOf(node);
        var output = new byte[length];
        var offset = writeMessage(node, output, 0);
        if(offset != length) {
            throw new InternalError("Unexpected mismatch between write position and message length");
        }
        return output;
    }

    /**
     * Writes a message header (0x00) followed by the encoded node.
     *
     * @param input the node to encode
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeMessage(Node input, byte[] output, int offset) {
        output[offset++] = 0;
        return writeNode(input, output, offset);
    }

    /**
     * Writes a complete node to the output array.
     * <p>
     * A node consists of:
     * <ol>
     *   <li>List size (number of attributes + 2 for description + children)</li>
     *   <li>Description string</li>
     *   <li>Attributes</li>
     *   <li>Content</li>
     * </ol>
     *
     * @param input the node to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeNode(Node input, byte[] output, int offset){
        offset = writeList(input.size(), output, offset);
        offset = writeString(input.description(), output, offset);
        offset = writeAttributes(input.attributes(), output, offset);
        offset = writeContent(input, output, offset);
        return offset;
    }

    /**
     * Writes a list size tag and value.
     *
     * @param size the size of the list
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     * @throws IllegalArgumentException if the size exceeds the maximum supported value
     */
    private static int writeList(int size, byte[] output, int offset) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            return writeList8((byte) size, output, offset);
        }else if (size < UNSIGNED_SHORT_MAX_VALUE) {
            return writeList16(size, output, offset);
        }else {
            throw new IllegalArgumentException("Cannot write list: overflow");
        }
    }

    /**
     * Writes an 8-bit list size (LIST_8 tag + size byte).
     *
     * @param size the size of the list
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeList8(byte size, byte[] output, int offset) {
        output[offset++] = LIST_8;
        output[offset++] = size;
        return offset;
    }

    /**
     * Writes a 16-bit list size (LIST_16 tag + two size bytes).
     *
     * @param size the size of the list
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeList16(int size, byte[] output, int offset) {
        output[offset++] = LIST_16;
        output[offset++] = (byte) (size >> 8);
        output[offset++] = (byte) size;
        return offset;
    }

    /**
     * Writes a string using the most efficient encoding method.
     * <p>
     * Encoding priority:
     * <ol>
     *   <li>Empty string → BINARY_8 + LIST_EMPTY</li>
     *   <li>Single-byte token → token index only</li>
     *   <li>Dictionary token → DICTIONARY_X + index</li>
     *   <li>UTF-8 string → binary length prefix + UTF-8 bytes</li>
     * </ol>
     *
     * @param input the string to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     * @throws RuntimeException if UTF-8 encoding fails
     */
    private static int writeString(String input, byte[] output, int offset){
        if (input.isEmpty()) {
            output[offset++] = BINARY_8;
            output[offset++] = LIST_EMPTY;
            return offset;
        }

        var singleByteTokenIndex = SINGLE_BYTE_TOKENS.indexOf(input);
        if (singleByteTokenIndex != -1) {
            output[offset++] = (byte) singleByteTokenIndex;
            return offset;
        }

        var dictionary0TokenIndex = DICTIONARY_0_TOKENS.indexOf(input);
        if (dictionary0TokenIndex != -1) {
            output[offset++] = DICTIONARY_0;
            output[offset++] = (byte) dictionary0TokenIndex;
            return offset;
        }

        var dictionary1TokenIndex = DICTIONARY_1_TOKENS.indexOf(input);
        if (dictionary1TokenIndex != -1) {
            output[offset++] = DICTIONARY_1;
            output[offset++] = (byte) dictionary1TokenIndex;
            return offset;
        }

        var dictionary2TokenIndex = DICTIONARY_2_TOKENS.indexOf(input);
        if (dictionary2TokenIndex != -1) {
            output[offset++] = DICTIONARY_2;
            output[offset++] = (byte) dictionary2TokenIndex;
            return offset;
        }

        var dictionary3TokenIndex = DICTIONARY_3_TOKENS.indexOf(input);
        if (dictionary3TokenIndex != -1) {
            output[offset++] = DICTIONARY_3;
            output[offset++] = (byte) dictionary3TokenIndex;
            return offset;
        }

        var length = calculateUtf8Length(input);
        offset = writeBinary(length, output, offset);
        var encoder = StandardCharsets.UTF_8.newEncoder();
        var inputBuffer = CharBuffer.wrap(input);
        var outputBuffer = ByteBuffer.wrap(output, offset, output.length - offset);
        var result = encoder.encode(inputBuffer, outputBuffer, true);
        if(result.isError()) {
            throw new RuntimeException("Cannot encode value: " + result);
        }
        return offset + length;
    }

    /**
     * Writes a binary length prefix with the appropriate size tag.
     *
     * @param input the length value to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBinary(int input, byte[] output, int offset) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            return writeBinary8((byte) input, output, offset);
        }else if (input < INT_20_MAX_VALUE) {
            return writeBinary20(input, output, offset);
        }else {
            return writeBinary32(input, output, offset);
        }
    }

    /**
     * Writes an 8-bit binary length (BINARY_8 tag + length byte).
     *
     * @param input the length value
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBinary8(byte input, byte[] output, int offset) {
        output[offset++] = BINARY_8;
        output[offset++] = input;
        return offset;
    }

    /**
     * Writes a 20-bit binary length (BINARY_20 tag + three length bytes).
     *
     * @param input the length value
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBinary20(int input, byte[] output, int offset) {
        output[offset++] = BINARY_20;
        output[offset++] = (byte) (input >> 16);
        output[offset++] = (byte) (input >> 8);
        output[offset++] = (byte) input;
        return offset;
    }

    /**
     * Writes a 32-bit binary length (BINARY_32 tag + four length bytes).
     *
     * @param input the length value
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBinary32(int input, byte[] output, int offset) {
        output[offset++] = BINARY_32;
        output[offset++] = (byte) (input >> 24);
        output[offset++] = (byte) (input >> 16);
        output[offset++] = (byte) (input >> 8);
        output[offset++] = (byte) input;
        return offset;
    }

    /**
     * Writes all node attributes as key-value pairs.
     *
     * @param attributes the attributes to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeAttributes(SequencedMap<String, ? extends NodeAttribute> attributes, byte[] output, int offset) {
        for (var entry : attributes.entrySet()) {
            offset = writeString(entry.getKey(), output, offset);
            offset = writeAttribute(entry.getValue(), output, offset);
        }
        return offset;
    }

    /**
     * Writes a single attribute value.
     *
     * @param attribute the attribute to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeAttribute(NodeAttribute attribute, byte[] output, int offset) {
        return switch (attribute) {
            case NodeAttribute.BytesAttribute(var buffer) -> writeBytes(buffer, output, offset);
            case NodeAttribute.JidAttribute(var jid) -> writeJid(jid, output, offset);
            case NodeAttribute.TextAttribute(var string) -> writeString(string, output, offset);
        };
    }

    /**
     * Writes the children of a node based on its type.
     *
     * @param content the node whose children to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeContent(Node content, byte[] output, int offset) {
        return switch (content) {
            case Node.BytesContent(var _, var _, var buffer) -> writeBytes(buffer, output, offset);
            case Node.ContainerNode(var _, var _, var children) -> writeChildren(children, output, offset);
            case Node.EmptyNode _ -> writeNull(output, offset);
            case Node.JidNode(var _, var _, var jid) -> writeJid(jid, output, offset);
            case Node.StreamNode(var _, var _, var inputStream, var inputStreamLength) -> writeStream(output, offset, inputStream, inputStreamLength);
            case Node.TextNode(var _, var _, var text) -> writeString(text, output, offset);
        };
    }

    /**
     * Writes a null/empty children marker (LIST_EMPTY).
     *
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeNull(byte[] output, int offset) {
        output[offset++] = LIST_EMPTY;
        return offset;
    }

    /**
     * Writes a collection of child nodes.
     *
     * @param values the child nodes to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeChildren(SequencedCollection<Node> values, byte[] output, int offset) {
        offset = writeList(values.size(), output, offset);
        for(var value : values) {
            offset = writeNode(value, output, offset);
        }
        return offset;
    }

    /**
     * Writes a byte array with a length prefix.
     *
     * @param buffer the byte array to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBytes(byte[] buffer, byte[] output, int offset){
        var length = buffer.length;
        offset = writeBinary(length, output, offset);
        System.arraycopy(buffer, 0, output, offset, length);
        return offset + length;
    }

    /**
     * Writes a ByteBuffer with a length prefix.
     *
     * @param buffer the ByteBuffer to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeBytes(ByteBuffer buffer, byte[] output, int offset){
        var length = buffer.remaining();
        offset = writeBinary(length, output, offset);
        buffer.get(output, offset, length);
        return offset + length;
    }

    /**
     * Writes a WhatsApp JID.
     * <p>
     * Two encoding formats:
     * <ul>
     *   <li>AD_JID: for JIDs with agent/device (tag + agent + device + user)</li>
     *   <li>JID_PAIR: standard format (tag + user/empty + server)</li>
     * </ul>
     *
     * @param jid the JID to write
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @return the new offset after writing
     */
    private static int writeJid(Jid jid, byte[] output, int offset){
        if (jid.hasAgent() || jid.hasDevice()) {
            output[offset++] = AD_JID;
            output[offset++] = (byte) jid.agent();
            output[offset++] = (byte) jid.device();
            return writeString(jid.user(), output, offset);
        }else {
            output[offset++] = JID_PAIR;
            if(jid.hasUser()) {
                offset = writeString(jid.user(), output, offset);
            }else {
                output[offset++] = LIST_EMPTY;
            }
            return writeString(jid.server().address(), output, offset);
        }
    }

    /**
     * Writes data from an InputStream with a length prefix.
     * <p>
     * Reads from the stream until the specified length is reached or EOF is encountered.
     *
     * @param output the output byte array
     * @param offset the current offset in the output array
     * @param inputStream the input stream to read from
     * @param length the number of bytes to read
     * @return the new offset after writing
     * @throws UncheckedIOException if an I/O error occurs while reading from the stream
     */
    private static int writeStream(byte[] output, int offset, InputStream inputStream, int length) {
        try {
            offset = writeBinary(length, output, offset);
            int read;
            while (length > 0 && (read = inputStream.read(output, offset, length)) != -1) {
                offset += read;
                length -= read;
            }
            if(length != 0) {
                throw new IllegalStateException("Unexpected EOF while reading stream");
            }
            return offset;
        }catch (IOException exception){
            throw new UncheckedIOException(exception);
        }
    }
}