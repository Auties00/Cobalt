package it.auties.whatsapp4j.binary;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class used to decode an encrypted BinaryArray received from WhatsappWeb's WebSocket.
 * To encode a message use {@link BinaryDecoder} instead.
 * Getting this class to work was not very easy, but I surely couldn't have done it without the help of:
 *
 * <ul>
 * <li>https://github.com/JicuNull/WhatsJava/blob/master/src/main/java/icu/jnet/whatsjava/encryption/BinaryDecoder.java - Java implementation, helped me to correctly cast a byte to an unsigned int, before I was using a method that just didn't work</li>
 * <li>https://github.com/adiwajshing/Baileys/blob/master/src/Binary/Decoder.ts - Typescript implementation, the logic was far less error prone than the one used by the python implementation on https://github.com/sigalor/whatsapp-web-reveng and the one I came up with</li>
 * </ul>
 */
public class BinaryDecoder {
    /**
     * The {@link BinaryArray} to decode
     */
    private BinaryArray buffer;

    /**
     * The current index
     */
    private int index;

    /**
     * Decodes {@code buffer} as a new {@link WhatsappNode}
     * This method should not be used by multiple concurrent threads on the same instance
     *
     * @param buffer the BinaryArray to decrypt
     * @return a new {@link WhatsappNode} containing all the information that was decrypted
     */
    public @NotNull WhatsappNode decodeDecryptedMessage(@NotNull BinaryArray buffer) {
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
        return switch (BinaryTag.forData(data)) {
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
            val |= readUnsignedInt() << (shift * 8);
        }

        return val;
    }

    private int readInt20() {
        checkEOS(3);
        var a = readUnsignedInt();
        var b = readUnsignedInt();
        var c = readUnsignedInt();
        return ((a & 15) << 16) + (b << 8) + c;
    }

    private @NotNull String readPacked8(int tag) {
        var startByte = readByte();

        final var value = new StringBuilder();
        for(var i = 0; i < (startByte & 127); i++) {
            var curByte = readByte();
            value.append(String.valueOf(Character.toChars(unpackByte(tag, ((curByte & 0xf0)) >> 4))));
            value.append(String.valueOf(Character.toChars(unpackByte(tag, (curByte & 0x0f)))));
        }

        return startByte >> 7 != 0 ? value.substring(0, value.length() - 1) : value.toString();
    }

    private @NotNull BinaryArray readBytes(int n) {
        checkEOS(n);
        var byteArray = buffer.slice(index, index + n);
        index += n;
        return byteArray;
    }

    private byte readByte() {
        checkEOS(1);
        return buffer.at(index++);
    }

    private int readUnsignedInt(){
        return Byte.toUnsignedInt(readByte());
    }

    private boolean isListTag(int tag) {
        return tag == BinaryTag.LIST_EMPTY.data() || tag == BinaryTag.LIST_8.data() || tag == BinaryTag.LIST_16.data();
    }

    private boolean isBinaryTag(int tag) {
        return tag == BinaryTag.BINARY_8.data() || tag == BinaryTag.BINARY_20.data() || tag == BinaryTag.BINARY_32.data();
    }

    private int readListSize(int data) {
        return switch (BinaryTag.forData(data)){
            case LIST_EMPTY -> 0;
            case LIST_8 -> readUnsignedInt();
            case LIST_16 -> readInt(2);
            default -> throw new IllegalStateException("BinaryReader#readListSize: unexpected tag: " + data);
        };
    }

    private @NotNull String readStringFromCharacters(int length) {
        checkEOS(length);
        var value = buffer.slice(index, index + length);
        index += length;
        return value.toString();
    }

    private String getToken(int index) {
        Validate.isTrue(index >= 3 && index < BinaryTokens.SINGLE_BYTE.size(), "Unexpected value: %s", index);
        return BinaryTokens.SINGLE_BYTE.get(index);
    }

    private String getDoubleToken(int index1, int index2) {
        var n = 256 * index1 + index2;
        Validate.isTrue(n >= 0 && n <= BinaryTokens.DOUBLE_BYTE.size(), "Unexpected value: " + n);
        return BinaryTokens.DOUBLE_BYTE.get(n);
    }

    private @NotNull String readString(int data) {
        return data >= 3 && data <= 235 ? getToken(data) : switch (BinaryTag.forData(data)) {
            case DICTIONARY_0, DICTIONARY_1, DICTIONARY_2, DICTIONARY_3 -> getDoubleToken(data - BinaryTag.DICTIONARY_0.data(), readByte());
            case BINARY_8 -> readStringFromCharacters(readByte());
            case BINARY_20 -> readStringFromCharacters(readInt20());
            case BINARY_32 -> readStringFromCharacters(readInt(4));
            case JID_PAIR -> "%s@%s".formatted(readString(readUnsignedInt()), readString(readUnsignedInt()));
            case NIBBLE_8, HEX_8 -> readPacked8(data);
            default -> throw new IllegalStateException("BinaryReader#readString: unexpected tag: " + data);
        };
    }

    @SneakyThrows
    private Map<String, String> readAttributes(int n) {
        return IntStream.range(0, n).boxed().collect(Collectors.toMap(x -> readString(readUnsignedInt()), x -> readString(readUnsignedInt()), (a, b) -> b, HashMap::new));
    }

    @SneakyThrows
    private @NotNull WhatsappNode readNode() {
        var listSize = readListSize(readUnsignedInt());
        Validate.isTrue(listSize != 0, "List size is empty");

        var descriptionTag = readUnsignedInt();
        Validate.isTrue(descriptionTag != BinaryTag.STREAM_END.data(), "Unexpected stream end");

        var description = readString(descriptionTag);
        var attrs = readAttributes((listSize - 1) >> 1);
        if (listSize % 2 != 0) {
            return new WhatsappNode(description, attrs, null);
        }

        var tag = readUnsignedInt();
        return new WhatsappNode(description, attrs, isListTag(tag) ? readList(tag) : isBinaryTag(tag) ? parseMessage(description, tag) : readString(tag));
    }

    @SneakyThrows
    private @NotNull Object parseMessage(@NotNull String description, int tag) {
        var data = switch (BinaryTag.forData(tag)) {
            case BINARY_8 -> readBytes(readUnsignedInt());
            case BINARY_20 -> readBytes(readInt20());
            case BINARY_32 -> readBytes(readInt(4));
            default -> throw new IllegalStateException("BinaryReader#readNode: unexpected tag: " + tag);
        };

        return description.equals("message") ? WhatsappProtobuf.WebMessageInfo.parseFrom(data.data()) : data.toString();
    }

    private @NotNull List<WhatsappNode> readList(int tag) {
        return IntStream.range(0, readListSize(tag)).mapToObj(e -> readNode()).toList();
    }

    private void checkEOS(int length) {
        Validate.isTrue(index + length <= buffer.size(), "End of stream!");
    }
}