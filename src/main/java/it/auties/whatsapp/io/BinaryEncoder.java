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

    private final byte[] output;
    private int offset;

    private BinaryEncoder(byte[] output, int offset) {
        this.output = output;
        this.offset = offset;
    }

    public static byte[] encode(Node node) {
        var length = BinaryLength.sizeOf(node);
        var output = new byte[length];
        var encoder = new BinaryEncoder(output, 0);
        encoder.writeMessage(node);
        return output;
    }

    public static void encode(Node node, byte[] output, int offset) {
        var encoder = new BinaryEncoder(output, offset);
        encoder.writeMessage(node);
    }

    private void writeMessage(Node input) {
        output[offset++] = 0;
        writeNode(input);
    }

    private void writeNode(Node input){
        writeList(input.size());
        writeString(input.description());
        writeAttributes(input.attributes());
        if(input.hasContent()) {
            writeContent(input.content());
        }
    }

    private void writeList(int size) {
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
    }

    private void writeString(String input){
        if (input.isEmpty()) {
            output[offset++] = BINARY_8;
            output[offset++] = LIST_EMPTY;
            return;
        }

        var singleByteTokenIndex = SINGLE_BYTE_TOKENS.indexOf(input);
        if (singleByteTokenIndex != -1) {
            output[offset++] = (byte) singleByteTokenIndex;
            return;
        }

        var dictionary0TokenIndex = DICTIONARY_0_TOKENS.indexOf(input);
        if (dictionary0TokenIndex != -1) {
            output[offset++] = DICTIONARY_0;
            output[offset++] = (byte) dictionary0TokenIndex;
            return;
        }

        var dictionary1TokenIndex = DICTIONARY_1_TOKENS.indexOf(input);
        if (dictionary1TokenIndex != -1) {
            output[offset++] = DICTIONARY_1;
            output[offset++] = (byte) dictionary1TokenIndex;
            return;
        }

        var dictionary2TokenIndex = DICTIONARY_2_TOKENS.indexOf(input);
        if (dictionary2TokenIndex != -1) {
            output[offset++] = DICTIONARY_2;
            output[offset++] = (byte) dictionary2TokenIndex;
            return;
        }

        var dictionary3TokenIndex = DICTIONARY_3_TOKENS.indexOf(input);
        if (dictionary3TokenIndex != -1) {
            output[offset++] = DICTIONARY_3;
            output[offset++] = (byte) dictionary3TokenIndex;
            return;
        }

        var length = Strings.utf8Length(input);
        writeBinary(length);
        var encoder = StandardCharsets.UTF_8.newEncoder();
        var inputBuffer = CharBuffer.wrap(input);
        var outputBuffer = ByteBuffer.wrap(output, offset, output.length - offset);
        var result = encoder.encode(inputBuffer, outputBuffer, true);
        if(result.isError()) {
            throw new RuntimeException("Cannot encode string: " + result);
        }
        offset += length;
    }

    private void writeBinary(long input) {
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
    }

    private void writeAttributes(Attributes attributes) {
        attributes.forEach((key, attribute) -> {
            writeString(key);
            writeContent(attribute);
        });
    }

    private void writeContent(Object input){
        switch (input) {
            case null -> output[offset++] = LIST_EMPTY;
            case String value -> writeString(value);
            case byte[] value -> writeBytes(value);
            case Boolean bool -> writeString(Boolean.toString(bool));
            case Number number -> writeString(number.toString());
            case Enum<?> value -> writeString(value.toString());
            case Jid value -> writeJid(value);
            case List<?> value -> writeChildren(value);
            default -> throw new RuntimeException("Invalid payload type");
        }
    }

    private void writeChildren(List<?> values) {
        writeList(values.size());
        for(var value : values) {
            try {
                writeNode((Node) value);
            }catch (ClassCastException ignored) {
                throw new RuntimeException("Invalid payload type");
            }
        }
    }

    private void writeBytes(byte[] bytes){
        var length = bytes.length;
        writeBinary(length);
        System.arraycopy(bytes, 0, output, offset, length);
        offset += length;
    }

    private void writeJid(Jid jid){
        if (jid.isCompanion()) {
            output[offset++] = COMPANION_JID;
            output[offset++] = (byte) jid.agent();
            output[offset++] = (byte) jid.device();
            writeString(jid.user());
        }else {
            output[offset++] = JID_PAIR;
            if(jid.hasUser()) {
                writeString(jid.user());
            }else {
                output[offset++] = LIST_EMPTY;
            }
            writeString(jid.server().address());
        }
    }
}
