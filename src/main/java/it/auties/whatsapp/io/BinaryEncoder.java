package it.auties.whatsapp.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Node;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static it.auties.whatsapp.io.BinaryTag.*;

public final class BinaryEncoder implements AutoCloseable {
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;

    private final ByteArrayOutputStream byteArrayOutputStream;
    private final DataOutputStream dataOutputStream;
    private final List<String> singleByteTokens;
    private final List<String> doubleByteTokens;
    private boolean closed;

    public BinaryEncoder() {
        this(BinaryTokens.SINGLE_BYTE, BinaryTokens.DOUBLE_BYTE);
    }

    public BinaryEncoder(List<String> singleByteTokens, List<String> doubleByteTokens) {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        this.singleByteTokens = singleByteTokens;
        this.doubleByteTokens = doubleByteTokens;
    }

    public byte[] encode(Node node) throws IOException {
        if(closed) {
            throw new IllegalStateException("The encoder is closed");
        }

        dataOutputStream.write(0);
        writeNode(node);
        return byteArrayOutputStream.toByteArray();
    }

    private void writeString(String input, BinaryTag token) throws IOException {
        dataOutputStream.write(token.data());
        writeStringLength(input);
        for (int charCode = 0, index = 0; index < input.length(); index++) {
            var stringCodePoint = Character.codePointAt(input, index);
            var binaryCodePoint = getStringCodePoint(token, stringCodePoint);

            if (index % 2 != 0) {
                dataOutputStream.write(charCode |= binaryCodePoint);
                continue;
            }

            charCode = binaryCodePoint << 4;
            if (index != input.length() - 1) {
                continue;
            }

            dataOutputStream.write(charCode |= 15);
        }
    }

    private int getStringCodePoint(BinaryTag token, int codePoint) {
        if (codePoint >= 48 && codePoint <= 57) {
            return codePoint - 48;
        }

        if (token == NIBBLE_8 && codePoint == 45) {
            return 10;
        }

        if (token == NIBBLE_8 && codePoint == 46) {
            return 11;
        }

        if (token == HEX_8 && codePoint >= 65 && codePoint <= 70) {
            return codePoint - 55;
        }

        throw new IllegalArgumentException("Cannot parse codepoint %s with token %s".formatted(codePoint, token));
    }

    private void writeStringLength(String input) throws IOException {
        var roundedLength = (int) Math.ceil(input.length() / 2F);
        if (input.length() % 2 == 1) {
            dataOutputStream.write(roundedLength | 128);
            return;
        }

        dataOutputStream.write(roundedLength);
    }

    private void writeLong(long input) throws IOException {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            dataOutputStream.write(BINARY_8.data());
            dataOutputStream.write((int) input);
            return;
        }

        if (input < INT_20_MAX_VALUE) {
            dataOutputStream.write(BINARY_20.data());
            dataOutputStream.write((int) ((input >>> 16) & 255));
            dataOutputStream.write((int) ((input >>> 8) & 255));
            dataOutputStream.write((int) (255 & input));
            return;
        }

        dataOutputStream.write(BINARY_32.data());
        dataOutputStream.writeLong(input);
    }

    private void writeString(String input) throws IOException {
        if (input.isEmpty()) {
            dataOutputStream.write(BINARY_8.data());
            dataOutputStream.write(LIST_EMPTY.data());
            return;
        }

        var tokenIndex = singleByteTokens.indexOf(input);
        if (tokenIndex != -1) {
            dataOutputStream.write(tokenIndex + 1);
            return;
        }

        if (writeDoubleByteString(input)) {
            return;
        }

        var length = length(input);
        if (length < 128 && !BinaryTokens.anyMatch(input, BinaryTokens.NUMBERS_REGEX)) {
            writeString(input, NIBBLE_8);
            return;
        }

        if (length < 128 && !BinaryTokens.anyMatch(input, BinaryTokens.HEX_REGEX)) {
            writeString(input, HEX_8);
            return;
        }

        writeLong(length);
        dataOutputStream.write(input.getBytes(StandardCharsets.UTF_8));
    }

    private boolean writeDoubleByteString(String input) throws IOException {
        if (!doubleByteTokens.contains(input)) {
            return false;
        }

        var index = doubleByteTokens.indexOf(input);
        dataOutputStream.write(doubleByteStringTag(index).data());
        dataOutputStream.write(index % (doubleByteTokens.size() / 4));
        return true;
    }

    private BinaryTag doubleByteStringTag(int index) {
        return switch (index / (doubleByteTokens.size() / 4)) {
            case 0 -> DICTIONARY_0;
            case 1 -> DICTIONARY_1;
            case 2 -> DICTIONARY_2;
            case 3 -> DICTIONARY_3;
            default -> throw new IllegalArgumentException("Cannot find tag for quadrant %s".formatted(index));
        };
    }

    private void writeNode(Node input) throws IOException {
        if (input.description().equals("0")) {
            dataOutputStream.write(LIST_8.data());
            dataOutputStream.write(LIST_EMPTY.data());
            return;
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        if (input.hasContent()) {
            write(input.content());
        }
    }

    private void writeAttributes(Node input) throws IOException {
        for (var entry : input.attributes().toMap().entrySet()) {
            writeString(entry.getKey());
            write(entry.getValue());
        }
    }

    private void writeInt(int size) throws IOException {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            dataOutputStream.write(LIST_8.data());
            dataOutputStream.write(size);
            return;
        }

        if (size < UNSIGNED_SHORT_MAX_VALUE) {
            dataOutputStream.write(LIST_16.data());
            dataOutputStream.writeShort(size);
            return;
        }

        throw new IllegalArgumentException("Cannot write int %s: overflow".formatted(size));
    }

    private void write(Object input) throws IOException {
        switch (input) {
            case null -> dataOutputStream.write(LIST_EMPTY.data());
            case String str -> writeString(str);
            case Boolean bool -> writeString(Boolean.toString(bool));
            case Number number -> writeString(number.toString());
            case byte[] bytes -> writeBytes(bytes);
            case Jid jid -> writeJid(jid);
            case Collection<?> collection -> writeList(collection);
            case Enum<?> serializable -> writeString(Objects.toString(serializable));
            case Node node ->
                    throw new IllegalArgumentException("Invalid payload type(nodes should be wrapped by a collection): %s".formatted(input));
            default ->
                    throw new IllegalArgumentException("Invalid payload type(%s): %s".formatted(input.getClass().getName(), input));
        }
    }

    private void writeList(Collection<?> collection) throws IOException {
        writeInt(collection.size());
        for (var entry : collection) {
            if (entry instanceof Node node) {
                writeNode(node);
            }
        }
    }

    private void writeBytes(byte[] bytes) throws IOException {
        writeLong(bytes.length);
        dataOutputStream.write(bytes);
    }

    private void writeJid(Jid jid) throws IOException {
        if (jid.isCompanion()) {
            dataOutputStream.write(COMPANION_JID.data());
            dataOutputStream.write(jid.agent());
            dataOutputStream.write(jid.device());
            writeString(jid.user());
            return;
        }

        dataOutputStream.write(JID_PAIR.data());
        if (jid.user() != null) {
            writeString(jid.user());
            writeString(jid.server().address());
            return;
        }

        dataOutputStream.write(LIST_EMPTY.data());
        writeString(jid.server().address());
    }

    private int length(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        dataOutputStream.close();
    }
}
