package it.auties.whatsapp4j.beta.serialization;

import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.common.binary.BinaryTag;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Inflater;

import static it.auties.whatsapp4j.beta.serialization.BinaryTokens.*;
import static it.auties.whatsapp4j.common.binary.BinaryTag.*;

@AllArgsConstructor
public class StanzaEncoder{
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;

    private Binary binary;
    private int counter;
    private String uuid;
    public StanzaEncoder(){
        this(new Binary(), 1, null);
    }

    private List<Node> readList(int size){
        return IntStream.range(0, size)
                .mapToObj(index -> decodeStanza(binary))
                .toList();
    }

    private String readString(List<Character> permitted, int start, int end) {
        var string = new int[2 * end - start];
        IntStream.iterate(0, index -> index < string.length - 1, n -> n + 2)
                .forEach(index -> readChar(permitted, string, index));
        if (start != 0) {
            string[string.length - 1] = permitted.get(binary.readUInt8() >>> 4);
        }

        return Arrays.stream(string)
                .mapToObj(e -> String.valueOf((char) e))
                .collect(Collectors.joining());
    }

    private void readChar(List<Character> permitted, int[] string, int index) {
        var token = binary.readUInt8();
        string[index] = permitted.get(token >>> 4);
        string[index + 1] = permitted.get(15 & token);
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

    private void writeNode(Node input) {
        if (input.description().equals("0")) {
            binary.writeUInt8(LIST_8);
            binary.writeUInt8(LIST_EMPTY);
            return;
        }

        writeInt(input.size());
        writeString(input.description());
        writeAttributes(input);
        write(input.content());
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

    private Object read(boolean parseBytes) {
        var token = binary.readUInt8();
        return switch (token) {
            case 0 -> null;
            case 247 -> readAdJid();
            case 248 -> readList(binary.readUInt8());
            case 249 -> readList(binary.readUInt16());
            case 250 -> readEncodedJid();
            case 251 -> readHexString();
            case 252 -> readString(parseBytes);
            case 253 -> readString16(parseBytes);
            case 254 -> readString32(parseBytes);
            case 255 -> readNumericString();
            default -> readStringFromToken(token);
        };
    }

    private String readStringFromToken(int token) {
        if (token >= DICTIONARY_0.data()) {
            return DOUBLE_BYTE.get(binary.readUInt8());
        }

        return SINGLE_BYTE.get(token - 1);
    }

    private String readNumericString() {
        var number = binary.readUInt8();
        return readString(NUMBERS, number >>> 7, 127 & number);
    }

    private Object readString32(boolean parseBytes) {
        if (parseBytes) {
            return binary.readString(binary.readUInt32());
        }

        return binary.readBytes(binary.readUInt32());
    }

    private Object readString16(boolean parseBytes) {
        var size = ((15 & binary.readUInt8()) << 16) + (binary.readUInt8() << 8) + binary.readUInt8();
        if (parseBytes) {
            return binary.readString(size);
        }

        return binary.readBytes(size);
    }

    private Object readString(boolean parseBytes) {
        if (parseBytes) {
            return binary.readString(binary.readUInt8());
        }

        return binary.readBytes(binary.readUInt8());
    }

    private String readHexString() {
        var number = binary.readUInt8();
        return readString(HEX, number >>> 7, 127 & number);
    }

    private Jid readEncodedJid() {
        var userObj = read(true);
        if(!(userObj instanceof String user)){
            throw new RuntimeException("Decode string got invalid value ${String(t)}, string expected");
        }

        // yield Jid.create(user, decodeStanzaString(Binary.fromString(user)));
        throw new UnsupportedOperationException(user);
    }

    private Jid readAdJid() {
        var agent = binary.readUInt8();
        var device = binary.readUInt8();
        var user = decodeStanzaString();
        return Jid.createAd(user, agent, device);
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

    public String decodeStanzaString() {
        var t = read(true);
        if(!(t instanceof String result)){
            throw new RuntimeException("Decode string got invalid value %s, string expected".formatted(t));
        }

        return result;
    }

    public Node decodeStanza(Binary binary){
        this.binary = binary;
        var token = binary.readUInt8();
        var size = readListSize(binary, token);
        if (size == 0) {
            throw new RuntimeException("Failed to decode node: node cannot be empty");
        }

        var description = decodeStanzaString();
        var attrs = readAttributes(size);
        if (size % 2 != 0) {
            return new Node(description, attrs, null);
        }

        var content = read(false);
        if (content instanceof Jid jid) {
            return new Node(description, attrs, jid.toString());
        }

        return new Node(description, attrs, content);
    }

    private int readListSize(Binary binary, int token) {
        if (token == LIST_8.data()){
            return binary.readUInt8();
        }

        return binary.readUInt16();
    }

    private Map<String, Object> readAttributes(int size) {
        var map = new HashMap<String, Object>();
        for (var t = size - 1; t > 1; t -= 2) {
            var key = decodeStanzaString();
            var value = read(true);
            map.put(key, value);
        }

        return map;
    }

    public byte[] encodeStanza(Node node) {
        writeNode(node);
        var binaryArr = binary.readWrittenBytes();
        var result = new byte[1 + binaryArr.length];
        result[0] = 0;
        System.arraycopy(binaryArr, 0, result, 1, binaryArr.length);
        return result;
    }

    public Binary unpackStanza(byte[] input) {
        var data = Binary.fromBytes(input);
        var token = data.readUInt8() & 2;
        if (token == 0) {
            return data;
        }

        try {
            var decompressor = new Inflater();
            decompressor.setInput(data.readWrittenBytes());
            var temp = new byte[2048];
            var length = decompressor.inflate(temp);
            var result = new byte[length];
            System.arraycopy(temp, 0, result, 0, length);
            return Binary.fromBytes(result);
        }catch (Exception exception){
            throw new RuntimeException("Cannot inflate data", exception);
        }
    }

    public String generateId() {
        if (this.uuid == null) {
            this.uuid = "%s.%s".formatted(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt());
        }

        return "%s-%s".formatted(uuid, counter++);
    }
}