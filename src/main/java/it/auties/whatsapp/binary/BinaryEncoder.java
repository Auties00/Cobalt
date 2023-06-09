package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.BytesHelper;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static it.auties.whatsapp.binary.BinaryTag.*;

public final class BinaryEncoder {
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;
    
    private final ByteBuf buffer;
    private final List<String> singleByteTokens;
    private final List<String> doubleByteTokens;

    public BinaryEncoder() {
        this(BinaryTokens.SINGLE_BYTE, BinaryTokens.DOUBLE_BYTE);
    }

    public BinaryEncoder(List<String> singleByteTokens, List<String> doubleByteTokens){
        this.buffer = BytesHelper.newBuffer();
        this.singleByteTokens = singleByteTokens;
        this.doubleByteTokens = doubleByteTokens;
    }

    public synchronized byte[] encode(Node node) {
        buffer.clear();
        var encoded = writeNode(node);
        var result = new byte[1 + encoded.length];
        result[0] = 0;
        System.arraycopy(encoded, 0, result, 1, encoded.length);
        return result;
    }

    private void writeString(String input, BinaryTag token) {
        buffer.writeByte(token.data());
        writeStringLength(input);

        for (int charCode = 0, index = 0; index < input.length(); index++) {
            var stringCodePoint = Character.codePointAt(input, index);
            var binaryCodePoint = getStringCodePoint(token, stringCodePoint);

            if (index % 2 != 0) {
                buffer.writeByte(charCode |= binaryCodePoint);
                continue;
            }

            charCode = binaryCodePoint << 4;
            if (index != input.length() - 1) {
                continue;
            }

            buffer.writeByte(charCode |= 15);
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

    private void writeStringLength(String input) {
        var roundedLength = (int) Math.ceil(input.length() / 2F);
        if (input.length() % 2 == 1) {
            buffer.writeByte(roundedLength | 128);
            return;
        }

        buffer.writeByte(roundedLength);
    }

    private void writeLong(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(BINARY_8.data());
            buffer.writeByte((int) input);
            return;
        }

        if (input < INT_20_MAX_VALUE) {
            buffer.writeByte(BINARY_20.data());
            buffer.writeByte((int) ((input >>> 16) & 255));
            buffer.writeByte((int) ((input >>> 8) & 255));
            buffer.writeByte((int) (255 & input));
            return;
        }

        buffer.writeByte(BINARY_32.data());
        buffer.writeLong(input);
    }

    private void writeString(String input) {
        if (input.isEmpty()) {
            buffer.writeByte(BINARY_8.data());
            buffer.writeByte(LIST_EMPTY.data());
            return;
        }

        var tokenIndex = singleByteTokens.indexOf(input);
        if (tokenIndex != -1) {
            buffer.writeByte(tokenIndex + 1);
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
        buffer.writeBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    private boolean writeDoubleByteString(String input) {
        if (!doubleByteTokens.contains(input)) {
            return false;
        }

        var index = doubleByteTokens.indexOf(input);
        buffer.writeByte(doubleByteStringTag(index).data());
        buffer.writeByte(index % (doubleByteTokens.size() / 4));
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

    private byte[] writeNode(Node input) {
        if (input.description().equals("0")) {
            buffer.writeByte(LIST_8.data());
            buffer.writeByte(LIST_EMPTY.data());
            return BytesHelper.readBuffer(buffer.resetReaderIndex());
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        if (input.hasContent()) {
            write(input.content());
        }

        return BytesHelper.readBuffer(buffer.resetReaderIndex());
    }

    private void writeAttributes(Node input) {
        input.attributes().toMap().forEach((key, value) -> {
            writeString(key);
            write(value);
        });
    }

    private void writeInt(int size) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(LIST_8.data());
            buffer.writeByte(size);
            return;
        }

        if (size < UNSIGNED_SHORT_MAX_VALUE) {
            buffer.writeByte(LIST_16.data());
            buffer.writeShort(size);
            return;
        }

        throw new IllegalArgumentException("Cannot write int %s: overflow".formatted(size));
    }

    private void write(Object input) {
        if (input == null) {
            buffer.writeByte(LIST_EMPTY.data());
        } else if (input instanceof String str) {
            writeString(str);
        } else if (input instanceof Boolean bool) {
            writeString(Boolean.toString(bool));
        } else if (input instanceof Number number) {
            writeString(number.toString());
        } else if (input instanceof byte[] bytes) {
            writeBytes(bytes);
        } else if (input instanceof ContactJid jid) {
            writeJid(jid);
        } else if (input instanceof Collection<?> collection) {
            writeList(collection);
        } else if (input instanceof Enum<?> serializable) {
            writeString(Objects.toString(serializable));
        } else if (input instanceof Node) {
            throw new IllegalArgumentException("Invalid payload type(nodes should be wrapped by a collection): %s".formatted(input));
        } else {
            throw new IllegalArgumentException("Invalid payload type(%s): %s".formatted(input.getClass().getName(), input));
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
        buffer.writeBytes(bytes);
    }

    private void writeJid(ContactJid jid) {
        if (jid.isCompanion()) {
            buffer.writeByte(COMPANION_JID.data());
            buffer.writeByte(jid.agent());
            buffer.writeByte(jid.device());
            writeString(jid.user());
            return;
        }

        buffer.writeByte(JID_PAIR.data());
        if (jid.user() != null) {
            writeString(jid.user());
            writeString(jid.server().address());
            return;
        }

        buffer.writeByte(LIST_EMPTY.data());
        writeString(jid.server().address());
    }

    private int length(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }
}