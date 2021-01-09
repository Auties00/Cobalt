package it.auties.whatsapp4j.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import it.auties.whatsapp4j.constant.Tag;
import it.auties.whatsapp4j.constant.Tokens;
import it.auties.whatsapp4j.utils.ProtoBuf.WebMessageInfo;
import lombok.SneakyThrows;

/*
    Getting this class to work was not very easy, but I surely couldn't have done it without the help of:
    https://github.com/JicuNull/WhatsJava/blob/master/src/main/java/icu/jnet/whatsjava/encryption/BinaryDecoder.java - Java implementation, helped me to correctly cast a byte to an unsigned int, before I was using a method that just didn't work
    https://github.com/adiwajshing/Baileys/blob/master/src/Binary/Decoder.ts - Typescript implementation, the logic was far less error prone than the one used by the python implementation on https://github.com/sigalor/whatsapp-web-reveng and the one I came up with.
 */
public class BinaryDecoder {
    private final Gson gson;
    private BytesArray buffer;
    private int index;
    public BinaryDecoder(){
        this.gson = new Gson();
    }

    public String decodeDecryptedMessage(BytesArray buffer) {
        this.buffer = buffer;
        this.index = 0;
        return readNode();
    }

    private int unpackNibble(int value) {
        return value >= 0 && value <= 9 ? '0' + value : switch (value) {
            case 10 -> (int) '-';
            case 11 -> (int) '.';
            case 15 -> (int) '\0';
            default -> 0;
        };
    }

    private int unpackHex(int value) {
        return value >= 0 && value <= 15 ? value < 10 ? '0' + value : 'A' + value - 10 : 0;
    }

    private int unpackByte(int data, int value) {
        return switch (Tag.forData(data)) {
            case NIBBLE_8 -> unpackNibble(value);
            case HEX_8 -> unpackHex(value);
            default -> throw new IllegalStateException("BinaryReader#unpackByte: unexpected tag: " + data);
        };
    }

    private int readInt(int n) {
        checkEOS(n);
        var val = 0;
        for(var i = 0; i < n; i++) {
            var shift = n - 1 - i;
            val |= next() << (shift * 8);
        }

        return val;
    }

    private int readInt20() {
        checkEOS(3);
        var a = next() & 0xff;
        var b = next() & 0xff;
        var c = next() & 0xff;
        return ((a & 15) << 16) + (b << 8) + c;
    }

    private String readPacked8(int tag) {
        var startByte = readByte();

        final var value = new StringBuilder();
        for(var i = 0; i < (startByte & 127); i++) {
            var curByte = readByte();
            value.append(String.valueOf(Character.toChars(unpackByte(tag, ((curByte & 0xf0)) >> 4))));
            value.append(String.valueOf(Character.toChars(unpackByte(tag, (curByte & 0x0f)))));
        }

        return startByte >> 7 != 0 ? value.substring(0, value.length() - 1) : value.toString();
    }

    private BytesArray readBytes(int n) {
        checkEOS(n);
        var byteArray = buffer.slice(index, index + n);
        index += n;
        return byteArray;
    }

    private byte readByte() {
        checkEOS(1);
        return next();
    }

    private boolean isListTag(int tag) {
        return tag == Tag.LIST_EMPTY.data() || tag == Tag.LIST_8.data() || tag == Tag.LIST_16.data();
    }

    private int readListSize(int data) {
        return switch (Tag.forData(data)){
            case LIST_EMPTY -> 0;
            case LIST_8 -> readByte();
            case LIST_16 -> readInt(2) & 0xff;
            default -> throw new IllegalStateException("BinaryReader#readListSize: unexpected tag: " + data);
        };
    }

    private String readStringFromCharacters(int length) {
        checkEOS(length);
        var value = buffer.slice(index, index + length);
        index += length;
        return new String(value.data());
    }

    private String getToken(int index) {
        Validate.isTrue(index >= 3 && index < Tokens.SINGLE_BYTE_TOKENS.length, "Unexpected value: %s", index);
        return Tokens.SINGLE_BYTE_TOKENS[index];
    }
    
    private String getDoubleToken(int index1, int index2) {
        var n = 256 * index1 + index2;
        Validate.isTrue(n >= 0 && n <= Tokens.DOUBLE_BYTE_TOKENS.length, "Unexpected value: " + n);
        return Tokens.DOUBLE_BYTE_TOKENS[n];
    }

    private String readString(int data) {
        if(data >= 3 && data <= 235) {
            var token = getToken(data);
            return token.equals("s.whatsapp.net") ? "c.us" : token;
        }

        return switch(Tag.forData(data)) {
            case DICTIONARY_0, DICTIONARY_1, DICTIONARY_2, DICTIONARY_3 -> getDoubleToken(data - Tag.DICTIONARY_0.data(), readByte());
            case BINARY_8 -> readStringFromCharacters(readByte());
            case BINARY_20 -> readStringFromCharacters(readInt20());
            case BINARY_32 -> readStringFromCharacters(readInt(4));
            case JID_PAIR -> {
                var left = readString(readByte() & 0xff);
                var right = readString(readByte() & 0xff);
                yield left != null && right != null ? "%s@%s".formatted(left, right) : readPacked8(data);
            }
            case NIBBLE_8, HEX_8 -> readPacked8(data);
            default -> throw new IllegalStateException("BinaryReader#readString: unexpected tag: " + data);
        };
    }

    private String readAttributes(int n) {
        if(n == 0){
            return null;
        }

        var result = IntStream.range(0, n).boxed().collect(Collectors.toMap(x -> readString(readByte() & 0xff), x -> readString(readByte() & 0xff), (a, b) -> b, HashMap::new));
        return gson.toJson(result);
    }


    private String readNode() {
        var listSize = readListSize(readByte() & 0xff);
        var descriptionTag = readByte() & 0xff;
        Validate.isTrue(descriptionTag != Tag.STREAM_END.data(), "Unexpected stream end");

        var description = readString(descriptionTag);
        var attrs = readAttributes((listSize - 1) >> 1);
        if (listSize % 2 != 0) {
            return "[\"%s\", %s]".formatted(description, attrs);
        }

        var tag = readByte() & 0xff;
        var content = isListTag(tag) ? readList(tag) : switch (Tag.forData(tag)) {
            case BINARY_8 -> parseMessage(readBytes(readByte() & 0xff), description);
            case BINARY_20 -> parseMessage(readBytes(readInt20()), description);
            case BINARY_32 -> parseMessage(readBytes(readInt(4)), description);
            default -> readString(tag);
        };

        return "[\"%s\", %s, %s]".formatted(description, attrs, content);
    }

    @SneakyThrows
    private String parseMessage(BytesArray data, String description){
        return description.equalsIgnoreCase("message") ? gson.toJson(WebMessageInfo.parseFrom(data.data())) : new String(data.data());
    }

    private String readList(int tag) {
        var arr = new String[readListSize(tag)];
        for(var i = 0; i < arr.length; i++) arr[i] = readNode();
        return Arrays.toString(arr);
    }

    private void checkEOS(int length) {
        Validate.isTrue(index + length <= buffer.size(), "End of stream!");
    }

    private byte next() {
        return buffer.at(index++);
    }
}