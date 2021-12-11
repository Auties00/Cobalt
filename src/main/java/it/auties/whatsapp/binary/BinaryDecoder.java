package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.util.Buffers;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Inflater;

import static it.auties.whatsapp.binary.BinaryTag.*;

public record BinaryDecoder(@NonNull ByteBuf buffer){
    public BinaryDecoder(){
        this(Buffers.newBuffer());
    }
    public Node decode(byte @NonNull [] input){
        buffer.clear()
                .writeBytes(unpack(input));
        return readNode();
    }

    private byte[] unpack(byte[] input){
        var data = Buffers.newBuffer()
                .writeBytes(input);
        var token = data.readByte() & 2;
        if (token == 0) {
            return Buffers.readBytes(data);
        }

        try {
            var decompressor = new Inflater();
            decompressor.setInput(Buffers.readBytes(data));
            var temp = new byte[2048];
            var length = decompressor.inflate(temp);
            return Arrays.copyOf(temp, length);
        }catch (Exception exception){
            throw new RuntimeException("Cannot inflate data", exception);
        }
    }

    private Node readNode() {
        var token = buffer.readUnsignedByte();
        var size = readSize(token);
        if (size == 0) {
            throw new IllegalArgumentException("Failed to decode body: body cannot be empty");
        }

        var description = readString();
        var attrs = readAttributes(size);
        return size % 2 != 0 ? Node.with(description, attrs, null)
                : Node.with(description, attrs, read(false));
    }

    public String readString() {
        var read = read(true);
        if (!(read instanceof String string)) {
            throw new IllegalArgumentException("Strict decoding failed: expected string, got %s with type %s".formatted(read, read.getClass().getName()));
        }

        return string;
    }

    private List<Node> readList(int size){
        return IntStream.range(0, size)
                .mapToObj(index -> readNode())
                .toList();
    }

    private String readString(List<Character> permitted, int start, int end) {
        var string = new int[2 * end - start];
        IntStream.iterate(0, index -> index < string.length - 1, n -> n + 2)
                .forEach(index -> readChar(permitted, string, index));
        if (start != 0) {
            string[string.length - 1] = permitted.get(buffer.readUnsignedByte() >>> 4);
        }

        return Arrays.stream(string)
                .mapToObj(e -> String.valueOf((char) e))
                .collect(Collectors.joining());
    }

    private void readChar(List<Character> permitted, int[] string, int index) {
        var token = buffer.readUnsignedByte();
        string[index] = permitted.get(token >>> 4);
        string[index + 1] = permitted.get(15 & token);
    }

    private Object read(boolean parseBytes) {
        var tag = buffer.readUnsignedByte();
        return switch (forData(tag)) {
            case LIST_EMPTY -> null;
            case COMPANION_JID -> readCompanionJid();
            case LIST_8 -> readList(buffer.readUnsignedByte());
            case LIST_16 -> readList(buffer.readUnsignedShort());
            case JID_PAIR -> readJidPair();
            case HEX_8 -> readHexString();
            case BINARY_8 -> readString(parseBytes);
            case BINARY_20 -> readString16(parseBytes);
            case BINARY_32 -> readString32(parseBytes);
            case NIBBLE_8 -> readNibble();
            default -> readStringFromToken(tag);
        };
    }

    private String readStringFromToken(int token) {
        if (token >= DICTIONARY_0.data() && token <= DICTIONARY_3.data()) {
            var delta = (BinaryTokens.DOUBLE_BYTE.size() / 4) * (token - DICTIONARY_0.data());
            return BinaryTokens.DOUBLE_BYTE.get(buffer.readUnsignedByte() + delta);
        }

        return BinaryTokens.SINGLE_BYTE.get(token - 1);
    }

    private String readNibble() {
        var number = buffer.readUnsignedByte();
        return readString(BinaryTokens.NUMBERS, number >>> 7, 127 & number);
    }

    private Object readString32(boolean parseBytes) {
        if (parseBytes) {
            var buffer = this.buffer.readBytes(this.buffer.readUnsignedShort());
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(buffer, buffer.readUnsignedShort());
    }

    private Object readString16(boolean parseBytes) {
        var size = ((15 & buffer.readUnsignedByte()) << 16) + (buffer.readUnsignedByte() << 8) + buffer.readUnsignedByte();
        if (parseBytes) {
            var buffer = this.buffer.readBytes(size);
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(buffer, size);
    }

    private Object readString(boolean parseBytes) {
        var size = buffer.readUnsignedByte();
        if (parseBytes) {
            var buffer = this.buffer.readBytes(size);
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(buffer, size);
    }

    private String readHexString() {
        var number = buffer.readUnsignedByte();
        return readString(BinaryTokens.HEX, number >>> 7, 127 & number);
    }

    private ContactJid readJidPair() {
        return switch (read(true)){
            case String encoded -> ContactJid.of(encoded, readString());
            case null -> ContactJid.of(null, readString());
            default -> throw new RuntimeException("Invalid jid type");
        };
    }

    private ContactJid readCompanionJid() {
        var agent = buffer.readUnsignedByte();
        var device = buffer.readUnsignedByte();
        var user = readString();
        return ContactJid.ofCompanion(user, device, agent);
    }

    private int readSize(int token) {
        if (token == LIST_8.data()){
            return buffer.readUnsignedByte();
        }

        return buffer.readUnsignedShort();
    }

    private Map<String, Object> readAttributes(int size) {
        var map = new HashMap<String, Object>();
        for (var pair = size - 1; pair > 1; pair -= 2) {
            var key = readString();
            var value = read(true);
            map.put(key, value);
        }

        return map;
    }
}