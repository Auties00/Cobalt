package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.constant.Tag;
import it.auties.whatsapp4j.constant.Tokens;
import it.auties.whatsapp4j.model.Node;

import java.util.*;

public class BinaryMessengerReader {
    private final BytesArray data;
    private int index;
    public BinaryMessengerReader(BytesArray data){
        this.data = data;
        this.index = 0;
    }

    public Node readNode() {
        var listSize = readListSize(Tag.forData(readByte()));
        var descriptionTag = Tag.forData(readByte());
        Validate.isTrue(descriptionTag != Tag.STREAM_END, "Unexpected stream end!");

        var description = readString(descriptionTag);
        Validate.isTrue(listSize != 0, "Invalid node!");

        var attrs = readAttributes((listSize - 1) >> 1);
        if (listSize % 2 == 1) {
            return new Node(description, attrs, null);
        }

        var tag = Tag.forData(readByte());
        var content = isListTag(tag) ? readList(tag) : switch (tag) {
            case BINARY_8 -> readBytes(readByte());
            case BINARY_20 -> readBytes(readInt20());
            case BINARY_32 -> readBytes(readInt32());
            default -> readString(tag);
        };

        return new Node(description, attrs, content);
    }

    private void checkEOS(int length) {
        Validate.isTrue(index + length <= data.size(), "End of stream!");
    }

    private byte readByte() {
        checkEOS(1);
        return data.at(index++);
    }

    private int readIntN(int n) {
        checkEOS(n);
        var ret = 0;
        for (int x = 0; x < n; x++) ret |= data.at(index + x) << ((n - 1 - x) * 8);
        index += n;
        return ret;
    }

    private int readInt16() {
        return readIntN(2);
    }

    private int readInt20() {
        this.checkEOS(3);
        var ret = ((data.at(index) & 15) << 16) + (data.at(index + 1) << 8) + data.at(index + 2);
        index += 3;
        return ret;
    }

    private int readInt32() {
        return readIntN(4);
    }

    private String readPacked8(Tag tag) {
        var startByte = readByte();
        int bound = startByte & 127;
        var builder = new StringBuilder();
        for (int i = 0; i < bound; i++) {
            var currByte = readByte();
            builder.append(unpackByte(tag, (currByte & 0xF0) >> 4) + unpackByte(tag, currByte & 0x0F));
        }

        var result = builder.toString();
        return startByte >> 7 != 0 ? result.substring(result.length() - 1) : result;
    }

    private int unpackByte(Tag tag, int value) {
        return switch (tag) {
            case NIBBLE_8 -> unpackNibble(value);
            case HEX_8 -> unpackHex(value);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    private char unpackNibble(int value) {
        if (value >= 0 && value <= 9) {
            return (char) (((int) '0') + value);
        }

        return switch (value) {
            case 10 -> '-';
            case 11 -> '.';
            case 15 -> '\0';
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }


    private char unpackHex(int value) {
        if (value < 0 || value > 15) throw new IllegalStateException("Unexpected value: " + value);
        return value < 10 ? (char) (((int) '0') + value) : (char) (((int) 'A') + value - 10);
    }

    private boolean isListTag(Tag tag) {
        return tag == Tag.LIST_EMPTY || tag == Tag.LIST_8 || tag == Tag.LIST_16;
    }

    private int readListSize(Tag tag) {
        return switch (tag) {
            case LIST_EMPTY -> 0;
            case LIST_8 -> readByte();
            case LIST_16 -> readInt16();
            default -> throw new IllegalStateException("Unexpected enum: " + tag.name());
        };
    }

    private String readString(Tag tag) {
        Validate.isTrue(tag.data() >= 0, "Invalid tag value(%s) for method readString", tag.data());
        if (tag.data() >= 3 && tag.data() <= 235) {
            var token = getToken(tag.data());
            return token.equals("s.whatsapp.net") ? "c.us" : token;
        }

        return switch (tag) {
            case DICTIONARY_0, DICTIONARY_1, DICTIONARY_2, DICTIONARY_3 -> getTokenDouble(tag.data() - Tag.DICTIONARY_0.data(), readByte());
            case LIST_EMPTY -> "";
            case BINARY_8 -> readStringFromChars(readByte()).toASCIIString();
            case BINARY_20 -> readStringFromChars(readInt20()).toASCIIString();
            case BINARY_32 -> readStringFromChars(readInt32()).toASCIIString();
            case JID_PAIR -> "%s@%s".formatted(readString(Tag.forData(readByte())), Tag.forData(readByte()));
            case NIBBLE_8, HEX_8 -> readPacked8(tag);
            default -> throw new IllegalStateException("Unexpected enum: " + tag.name());
        };
    }

    private BytesArray readStringFromChars(int length) {
        checkEOS(length);
        var ret = data.slice(index, index + length);
        index += length;
        return ret;
    }

    private Map<String, String> readAttributes(int n) {
        Validate.isTrue(n != 0, "Unexpected value: %s", n);
        var data = new HashMap<String, String>();
        for (var x = 0; x < n; x++) {
            var index = readString(Tag.forData(readByte()));
            data.put(index, readString(Tag.forData(readByte())));
        }

        return data;
    }

    private List<Object> readList(Tag tag) {
        final var ret = new ArrayList<>();
        for (var x = 0; x < readListSize(tag); x++) ret.add(readNode());
        return ret;
    }

    private byte[] readBytes(int n) {
        var ret = new byte[n];
        for (var x = 0; x < n; x++) ret[x] = readByte();
        return ret;
    }

    private String getToken(int index) {
        Validate.isTrue(index >= 3 && index < Tokens.SINGLE_BYTE_TOKENS.size(), "Unexpected value: %s", index);
        return Tokens.SINGLE_BYTE_TOKENS.get(index);
    }

    private String getTokenDouble(int index1, int index2) {
        var n = 256 * index1 + index2;
        Validate.isTrue(n >= 0 && n < Tokens.DOUBLE_BYTE_TOKENS.size(), "Unexpected value: " + n);
        return Tokens.DOUBLE_BYTE_TOKENS.get(n);
    }
}