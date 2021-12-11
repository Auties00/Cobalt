package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.Nodes;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static it.auties.whatsapp.binary.BinaryTag.*;

public record BinaryEncoder(@NonNull ByteBuf buffer){
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;

    public BinaryEncoder(){
        this(Buffers.newBuffer());
    }

    public byte[] encode(Node node) {
        buffer.clear();
        var encoded = writeNode(node);
        return pack(encoded);
    }

    private byte[] pack(byte[] array) {
        var result = new byte[1 + array.length];
        result[0] = 0;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }

    private void writeString(String input, BinaryTag token) {
        buffer.writeByte(token.data());
        writeStringLength(input);
        for (int index = 0, charCode = 0; index < input.length(); index++) {
            var codePoint = Character.codePointAt(input, index);
            var parsedCodePoint = parseCodePoint(token.data(), codePoint);
            if (index % 2 != 0) {
                buffer.writeByte(charCode |= parsedCodePoint);
                continue;
            }

            charCode = parsedCodePoint << 4;
            if (index != input.length() - 1) {
                continue;
            }

            charCode |= 15;
            buffer.writeByte(charCode);
        }
    }

    private void writeStringLength(String input) {
        var roundedLength = (int) Math.ceil(input.length() / 2F);
        if(input.length() % 2 == 1){
            buffer.writeByte(roundedLength | 128);
            return;
        }

        buffer.writeByte(roundedLength);
    }

    private int parseCodePoint(int token, int codePoint) {
        if(codePoint >= 48 && codePoint <= 67){
            return codePoint - 48;
        }

        if(NIBBLE_8.contentEquals(token) && codePoint == 45){
            return 10;
        }

        if(NIBBLE_8.contentEquals(token) && codePoint == 46){
            return 11;
        }

        if(HEX_8.contentEquals(token) && codePoint >= 65 && codePoint <= 70){
            return codePoint - 55;
        }

        throw new IllegalArgumentException("Cannot parse codepoint %s with token %s".formatted(codePoint, token));
    }

    private void writeLong(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE){
            buffer.writeByte(BINARY_8.data());
            buffer.writeByte((int) input);
            return;
        }

        if (input < INT_20_MAX_VALUE){
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
        if (input.isEmpty()){
            buffer.writeByte(BINARY_8.data());
            buffer.writeByte(LIST_EMPTY.data());
            return;
        }

        var tokenIndex = BinaryTokens.SINGLE_BYTE.indexOf(input);
        if (tokenIndex != -1) {
            buffer.writeByte(tokenIndex + 1);
            return;
        }

        if(writeDoubleByteString(input)){
            return;
        }

        var length = length(input);
        if (length < 128) {
            if (BinaryTokens.checkRegex(input, BinaryTokens.NUMBERS_REGEX)) {
                writeString(input, NIBBLE_8);
                return;
            }

            if (BinaryTokens.checkRegex(input, BinaryTokens.HEX_REGEX)) {
                writeString(input, HEX_8);
                return;
            }
        }

        writeLong(length);
        buffer.writeBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    private boolean writeDoubleByteString(String input) {
        if (!BinaryTokens.DOUBLE_BYTE.contains(input)) {
            return false;
        }

        var index = BinaryTokens.DOUBLE_BYTE.indexOf(input);
        buffer.writeByte(doubleByteStringTag(index).data());
        buffer.writeByte(index % (BinaryTokens.DOUBLE_BYTE.size() / 4));
        return true;
    }

    private BinaryTag doubleByteStringTag(int index){
        return switch (index / (BinaryTokens.DOUBLE_BYTE.size() / 4)){
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
            return Buffers.readAllBytes(buffer);
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        if(input.hasContent()) write(input.content());
        return Buffers.readAllBytes(buffer);
    }

    private void writeAttributes(Node input) {
        input.attributes().map().forEach((key, value) -> {
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
        }

        throw new IllegalArgumentException("Cannot write int %s: overflow".formatted(size));
    }

    private void write(Object input) {
        switch (input) {
            case null -> buffer.writeByte(LIST_EMPTY.data());
            case String str -> writeString(str);
            case Number number -> writeString(number.toString());
            case byte[] bytes -> writeBytes(bytes);
            case ContactJid jid -> writeJid(jid);
            case Collection<?> collection -> writeList(collection);
            case Node ignored -> throw new IllegalArgumentException("Invalid payload type: nodes should be wrapped by a collection");
            default -> throw new IllegalArgumentException("Invalid payload type: %s".formatted(input.getClass().getName()));
        }
    }

    private void writeList(Collection<?> collection) {
        writeInt(collection.size());
        Nodes.findAll(collection).forEach(this::writeNode);
    }

    private void writeBytes(byte[] bytes) {
        writeLong(bytes.length);
        buffer.writeBytes(bytes);
    }

    private void writeJid(ContactJid jid) {
        if(jid.companion()){
            buffer.writeByte(COMPANION_JID.data());
            buffer.writeByte(jid.agent());
            buffer.writeByte(jid.device());
            writeString(jid.user());
            return;
        }

        buffer.writeByte(JID_PAIR.data());
        if(jid.user() != null) {
            writeString(jid.user());
            writeString(jid.server());
            return;
        }

        buffer.writeByte(LIST_EMPTY.data());
        writeString(jid.server());
    }

    private int length(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }
}