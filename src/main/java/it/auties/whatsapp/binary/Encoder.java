package it.auties.whatsapp.binary;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

import static it.auties.whatsapp.binary.Tag.*;

public class Encoder {
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;

    private Bytes buffer;

    public synchronized byte[] encode(Node node) {
        this.buffer = Bytes.newBuffer();
        var encoded = writeNode(node);
        var result = new byte[1 + encoded.length];
        result[0] = 0;
        System.arraycopy(encoded, 0, result, 1, encoded.length);
        return result;
    }

    private void writeString(String input, Tag token) {
        this.buffer = buffer.append(token.data());
        writeStringLength(input);

        for (int charCode = 0, index = 0; index < input.length(); index++) {
            var stringCodePoint = Character.codePointAt(input, index);
            var binaryCodePoint = getStringCodePoint(token, stringCodePoint);

            if (index % 2 != 0) {
                this.buffer = buffer.append(charCode |= binaryCodePoint);
                continue;
            }

            charCode = binaryCodePoint << 4;
            if (index != input.length() - 1) {
                continue;
            }

            this.buffer = buffer.append(charCode |= 15);
        }
    }

    private int getStringCodePoint(Tag token, int codePoint) {
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

    private void writeStringLength(String input) {
        var roundedLength = (int) Math.ceil(input.length() / 2F);
        if (input.length() % 2 == 1) {
            this.buffer = buffer.append(roundedLength | 128);
            return;
        }

        this.buffer = buffer.append(roundedLength);
    }

    private void writeLong(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            this.buffer = buffer.append(BINARY_8.data());
            this.buffer = buffer.append((int) input);
            return;
        }

        if (input < INT_20_MAX_VALUE) {
            this.buffer = buffer.append(BINARY_20.data());
            this.buffer = buffer.append((int) ((input >>> 16) & 255));
            this.buffer = buffer.append((int) ((input >>> 8) & 255));
            this.buffer = buffer.append((int) (255 & input));
            return;
        }

        this.buffer = buffer.append(BINARY_32.data());
        buffer.appendLong(input);
    }

    private void writeString(String input) {
        if (input.isEmpty()) {
            this.buffer = buffer.append(BINARY_8.data());
            this.buffer = buffer.append(LIST_EMPTY.data());
            return;
        }

        var tokenIndex = Tokens.SINGLE_BYTE.indexOf(input);
        if (tokenIndex != -1) {
            this.buffer = buffer.append(tokenIndex + 1);
            return;
        }

        if (writeDoubleByteString(input)) {
            return;
        }

        var length = length(input);
        if (length < 128 && !Tokens.anyMatch(input, Tokens.NUMBERS_REGEX)) {
            writeString(input, NIBBLE_8);
            return;
        }

        if (length < 128 && !Tokens.anyMatch(input, Tokens.HEX_REGEX)) {
            writeString(input, HEX_8);
            return;
        }

        writeLong(length);
        this.buffer = buffer.append(input.getBytes(StandardCharsets.UTF_8));
    }

    private boolean writeDoubleByteString(String input) {
        if (!Tokens.DOUBLE_BYTE.contains(input)) {
            return false;
        }

        var index = Tokens.DOUBLE_BYTE.indexOf(input);
        this.buffer = buffer.append(doubleByteStringTag(index).data());
        this.buffer = buffer.append(index % (Tokens.DOUBLE_BYTE.size() / 4));
        return true;
    }

    private Tag doubleByteStringTag(int index) {
        return switch (index / (Tokens.DOUBLE_BYTE.size() / 4)) {
            case 0 -> DICTIONARY_0;
            case 1 -> DICTIONARY_1;
            case 2 -> DICTIONARY_2;
            case 3 -> DICTIONARY_3;
            default -> throw new IllegalArgumentException("Cannot find tag for quadrant %s".formatted(index));
        };
    }

    private byte[] writeNode(Node input) {
        if (input.description()
                .equals("0")) {
            this.buffer = buffer.append(LIST_8.data());
            this.buffer = buffer.append(LIST_EMPTY.data());
            return buffer.toByteArray();
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        if (input.hasContent()) {
            write(input.content());
        }

        return buffer.toByteArray();
    }

    private void writeAttributes(Node input) {
        input.attributes()
                .map()
                .forEach((key, value) -> {
                    writeString(key);
                    write(value);
                });
    }

    private void writeInt(int size) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            this.buffer = buffer.append(LIST_8.data());
            this.buffer = buffer.append(size);
            return;
        }

        if (size < UNSIGNED_SHORT_MAX_VALUE) {
            this.buffer = buffer.append(LIST_16.data());
            buffer.appendShort(size);
            return;
        }

        throw new IllegalArgumentException("Cannot write int %s: overflow".formatted(size));
    }

    private void write(Object input) {
        switch (input) {
            case null -> this.buffer = buffer.append(LIST_EMPTY.data());
            case String str -> writeString(str);
            case Boolean bool -> writeString(Boolean.toString(bool));
            case Number number -> writeString(number.toString());
            case byte[] bytes -> writeBytes(bytes);
            case ContactJid jid -> writeJid(jid);
            case Collection<?> collection -> writeList(collection);
            case Enum<?> serializable -> writeString(Objects.toString(serializable));
            case Node ignored -> throw new IllegalArgumentException(
                    "Invalid payload type(nodes should be wrapped by a collection): %s".formatted(input));
            default -> throw new IllegalArgumentException("Invalid payload type(%s): %s".formatted(input.getClass()
                                                                                                           .getName(),
                                                                                                   input));
        }
    }

    private void writeList(Collection<?> collection) {
        writeInt(collection.size());
        collection.stream()
                .filter(entry -> entry instanceof Node)
                .map(entry -> (Node) entry)
                .forEach(this::writeNode);
    }

    private void writeBytes(byte[] bytes) {
        writeLong(bytes.length);
        this.buffer = buffer.append(bytes);
    }

    private void writeJid(ContactJid jid) {
        if (jid.isCompanion()) {
            this.buffer = buffer.append(COMPANION_JID.data());
            this.buffer = buffer.append(jid.agent());
            this.buffer = buffer.append(jid.device());
            writeString(jid.user());
            return;
        }

        this.buffer = buffer.append(JID_PAIR.data());
        if (jid.user() != null) {
            writeString(jid.user());
            writeString(jid.server()
                                .address());
            return;
        }

        this.buffer = buffer.append(LIST_EMPTY.data());
        writeString(jid.server()
                            .address());
    }

    private int length(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }
}