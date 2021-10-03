package it.auties.whatsapp4j.beta.binary;

import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.common.binary.BinaryTag;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static it.auties.whatsapp4j.beta.binary.BinaryTokens.*;
import static it.auties.whatsapp4j.common.binary.BinaryTag.*;

public record BinaryEncoder(BinaryBuffer binary){
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    public BinaryEncoder(){
        this(new BinaryBuffer());
    }

    public byte[] encode(Node node) {
        binary.buffer().reset();
        var binaryArr = writeNode(node);
        var result = new byte[1 + binaryArr.length];
        result[0] = 0;
        System.arraycopy(binaryArr, 0, result, 1, binaryArr.length);
        return result;
    }

    private void writeString(String input, BinaryTag token) {
        binary.writeUInt8(token);
        writeStringLength(input);
        for (int index = 0, charCode = 0; index < input.length(); index++) {
            var codePoint = Character.codePointAt(input, index);
            var parsedCodePoint = parseCodePoint(token.data(), codePoint);
            if (index % 2 != 0) {
                binary.writeUInt8(charCode |= parsedCodePoint);
                continue;
            }

            charCode = parsedCodePoint << 4;
            if (index != input.length() - 1) {
                continue;
            }

            charCode |= 15;
            binary.writeUInt8(charCode);
        }
    }

    private void writeStringLength(String input) {
        var roundedLength = (int) Math.ceil(input.length() / 2F);
        if(input.length() % 2 == 1){
            binary.writeUInt8(roundedLength | 128);
            return;
        }

        binary.writeUInt8(roundedLength);
    }

    private int parseCodePoint(int token, int codePoint) {
        if(codePoint >= 48 && codePoint <= 67){
            return codePoint - 48;
        }

        if(token == 255){
            if(codePoint == 45){
                return 10;
            }

            if(codePoint == 46){
                return 11;
            }
        }

        if(token == 251 && codePoint >= 65 && codePoint <= 70){
            return codePoint - 55;
        }

        throw new IllegalArgumentException("Cannot parse codepoint %s with token %s".formatted(codePoint, token));
    }

    private void writeLong(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE){
            binary.writeUInt8(BinaryTag.BINARY_8);
            binary.writeUInt8((int) input);
            return;
        }

        if (input < 1048576){
            binary.writeUInt8(BinaryTag.BINARY_20);
            binary.writeUInt8((int) ((input >>> 16) & 255));
            binary.writeUInt8((int) ((input >>> 8) & 255));
            binary.writeUInt8((int) (255 & input));
            return;
        }

        binary.writeUInt8(BinaryTag.BINARY_32);
        binary.writeUInt32(input);
    }

    private void writeString(String input) {
        if (input.isEmpty()){
            binary.writeUInt8(BinaryTag.BINARY_8);
            binary.writeUInt8(0);
            return;
        }

        var tokenIndex = SINGLE_BYTE.indexOf(input);
        if (tokenIndex != -1) {
            binary.writeUInt8(tokenIndex + 1);
            return;
        }

        if(writeDoubleByteString(input)){
            return;
        }

        var length = rawStringLength(input);
        if (length < 128) {
            if (!input.matches(NUMBERS_REGEX)) {
                writeString(input, BinaryTag.NIBBLE_8);
                return;
            }

            if (!input.matches(HEX_REGEX)) {
                writeString(input, BinaryTag.HEX_8);
                return;
            }
        }

        writeLong(length);
        binary.writeString(input);
    }

    private boolean writeDoubleByteString(String input) {
        if (!DOUBLE_BYTE.contains(input)) {
            return false;
        }

        var index = DOUBLE_BYTE.indexOf(input);
        binary.writeUInt8(index / (DOUBLE_BYTE.size() / 3));
        binary.writeUInt8(index);
        return true;
    }

    private int rawStringLength(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }

    private byte[] writeNode(Node input) {
        if (input.description().equals("0")) {
            binary.writeUInt8(LIST_8);
            binary.writeUInt8(LIST_EMPTY);
            return binary.readWrittenBytes();
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        write(input.content());
        return binary.readWrittenBytes();
    }

    private void writeAttributes(Node input) {
        input.attrs().forEach((key, value) -> {
            writeString(key);
            write(value);
        });
    }

    private void writeInt(int size) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            binary.writeUInt8(LIST_8);
            binary.writeUInt8(size);
            return;
        }

        if (size < UNSIGNED_SHORT_MAX_VALUE) {
            binary.writeUInt8(LIST_16);
            binary.writeUInt16(size);
        }

        throw new IllegalArgumentException("Cannot write int %s: overflow".formatted(size));
    }

    private void write(Object input) {
        switch (input) {
            case null -> binary.writeUInt8(0);
            case Jid jid -> writeJid(jid);
            case String str -> writeString(str);
            case byte[] bytes -> writeBytes(bytes);
            case Collection<?> collection -> writeList(collection);
            default -> throw new RuntimeException("Invalid payload type: %s".formatted(input.getClass().getName()));
        }
    }

    private void writeList(Collection<?> collection) {
        writeInt(collection.size());
        Node.fromGenericList(collection)
                .forEach(this::writeNode);
    }

    private void writeBytes(byte[] bytes) {
        writeLong(bytes.length);
        binary.writeBytes(bytes);
    }

    private void writeJid(Jid jid) {
        if(jid.ad()){
            binary.writeUInt8(247);
            binary.writeUInt8(jid.agent());
            binary.writeUInt8(jid.device());
            writeString(jid.user());
            return;
        }

        binary.writeUInt8(250);
        if(jid.user() != null) {
            write(jid.user());
            write(jid.server());
            return;
        }

        binary.writeUInt8(0);
        write(jid.server());
    }
}