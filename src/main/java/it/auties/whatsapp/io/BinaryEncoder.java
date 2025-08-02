package it.auties.whatsapp.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Strings;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.auties.whatsapp.io.BinaryTag.*;
import static it.auties.whatsapp.io.BinaryTokens.*;

public final class BinaryEncoder {
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;

    private BinaryEncoder() {
        // Utility class
    }

    public static byte[] encode(Node node) {
        var length = BinaryLength.sizeOf(node);
        var output = new byte[length];
        var offset = writeMessage(node, output, 0);
        if(offset != length) {
            throw new InternalError("Unexpected mismatch between write position and message length");
        }
        return output;
    }

    public static void encode(Node node, byte[] output, int offset) {
        writeMessage(node, output, offset);
    }

    private static int writeMessage(Node input, byte[] output, int offset) {
        output[offset++] = 0;
        return writeNode(input, output, offset);
    }

    private static int writeNode(Node input, byte[] output, int offset){
        offset = writeList(input.size(), output, offset);
        offset = writeString(input.description(), output, offset);
        offset = writeAttributes(input.attributes(), output, offset);
        if(input.hasContent()) {
            offset = writeContent(input.content(), output, offset);
        }
        return offset;
    }

    private static int writeList(int size, byte[] output, int offset) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            output[offset++] = LIST_8;
            output[offset++] = (byte) size;
        }else if (size < UNSIGNED_SHORT_MAX_VALUE) {
            output[offset++] = LIST_16;
            output[offset++] = (byte) (size >> 8);
            output[offset++] = (byte) size;
        }else {
            throw new IllegalArgumentException("Cannot write list: overflow");
        }
        return offset;
    }

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

        var length = Strings.utf8Length(input);
        offset = writeBinary(length, output, offset);
        var encoder = StandardCharsets.UTF_8.newEncoder();
        var inputBuffer = CharBuffer.wrap(input);
        var outputBuffer = ByteBuffer.wrap(output, offset, output.length - offset);
        var result = encoder.encode(inputBuffer, outputBuffer, true);
        if(result.isError()) {
            throw new RuntimeException("Cannot encode string: " + result);
        }
        offset += length;
        return offset;
    }

    private static int writeBinary(int input, byte[] output, int offset) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            output[offset++] = BINARY_8;
            output[offset++] = (byte) input;
        }else if (input < INT_20_MAX_VALUE) {
            output[offset++] = BINARY_20;
            output[offset++] = (byte) (input >> 16);
            output[offset++] = (byte) (input >> 8);
            output[offset++] = (byte) input;
        }else {
            output[offset++] = BINARY_32;
            output[offset++] = (byte) (input >> 24);
            output[offset++] = (byte) (input >> 16);
            output[offset++] = (byte) (input >> 8);
            output[offset++] = (byte) input;
        }
        return offset;
    }

    private static int writeAttributes(Attributes attributes, byte[] output, int offset) {
        for (var entry : attributes.toEntries()) {
            offset = writeString(entry.getKey(), output, offset);
            offset = writeContent(entry.getValue(), output, offset);
        }
        return offset;
    }

    private static int writeContent(Object input, byte[] output, int offset){
        return switch (input) {
            case null -> {
                output[offset++] = LIST_EMPTY;
                yield offset;
            }
            case String value -> writeString(value, output, offset);
            case byte[] value -> writeBytes(value, output, offset);
            case Boolean bool -> writeString(Boolean.toString(bool), output, offset);
            case Number number -> writeString(number.toString(), output, offset);
            case Enum<?> value -> writeString(value.toString(), output, offset);
            case Jid value -> writeJid(value, output, offset);
            case List<?> value -> writeChildren(value, output, offset);
            default -> throw new RuntimeException("Invalid payload type");
        };
    }

    private static int writeChildren(List<?> values, byte[] output, int offset) {
        offset = writeList(values.size(), output, offset);
        for(var value : values) {
            try {
                offset = writeNode((Node) value, output, offset);
            }catch (ClassCastException ignored) {
                throw new RuntimeException("Invalid payload type");
            }
        }
        return offset;
    }

    private static int writeBytes(byte[] bytes, byte[] output, int offset){
        var length = bytes.length;
        offset = writeBinary(length, output, offset);
        System.arraycopy(bytes, 0, output, offset, length);
        offset += length;
        return offset;
    }

    private static int writeJid(Jid jid, byte[] output, int offset){
        if (jid.isCompanion()) {
            output[offset++] = COMPANION_JID;
            output[offset++] = (byte) jid.agent();
            output[offset++] = (byte) jid.device();
            offset = writeString(jid.user(), output, offset);
        }else {
            output[offset++] = JID_PAIR;
            if(jid.hasUser()) {
                offset = writeString(jid.user(), output, offset);
            }else {
                output[offset++] = LIST_EMPTY;
            }
            offset = writeString(jid.server().address(), output, offset);
        }
        return offset;
    }
}