package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.socket.Node;
import it.auties.whatsapp.util.Buffers;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.auties.whatsapp.binary.BinaryTag.*;
import static it.auties.whatsapp.protobuf.contact.ContactJid.Server.forAddress;
import static it.auties.whatsapp.socket.Node.with;
import static it.auties.whatsapp.socket.Node.withAttributes;

public record BinaryDecoder(@NonNull ByteBuf buffer){
    public BinaryDecoder(){
        this(Buffers.newBuffer());
    }
    public Node decode(byte @NonNull [] input){
        buffer.clear().writeBytes(unpack(input));
        return readNode();
    }

    private byte[] unpack(byte[] input){
        var data = Buffers.newBuffer()
                .writeBytes(input);
        var token = data.readByte() & 2;
        if (token == 0) {
            return Buffers.readBytes(data);
        }

        var bytes = Buffers.readBytes(data);
        return SignalHelper.deflate(bytes);
    }

    private Node readNode() {
        var token = buffer.readUnsignedByte();
        var size = readSize(token);
        Validate.isTrue(size != 0,
                "Cannot decode node with empty body");
        var description = readString();
        var attrs = readAttributes(size);
        return size % 2 != 0 ? withAttributes(description, attrs)
                : with(description, attrs, read(false));
    }

    public String readString() {
        var read = read(true);
        if (read instanceof String string) {
            return string;
        }

        throw new IllegalArgumentException("Strict decoding failed: expected string, got %s with type %s"
                .formatted(read, read == null ? null : read.getClass().getName()));
    }

    private List<Node> readList(int size){
        return IntStream.range(0, size)
                .mapToObj(index -> readNode())
                .toList();
    }

    private String readString(List<Character> permitted, int start, int end) {
        var string = new char[2 * end - start];
        IntStream.iterate(0, index -> index < string.length - 1, n -> n + 2)
                .forEach(index -> readChar(permitted, string, index));
        if (start != 0) {
            string[string.length - 1] = permitted.get(buffer.readUnsignedByte() >>> 4);
        }

        return String.valueOf(string);
    }

    private void readChar(List<Character> permitted, char[] string, int index) {
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
            case BINARY_8 -> readString(buffer.readUnsignedByte(), parseBytes);
            case BINARY_20 -> readString(readString20Length(), parseBytes);
            case BINARY_32 -> readString(buffer.readUnsignedShort(), parseBytes);
            case NIBBLE_8 -> readNibble();
            default -> readStringFromToken(tag);
        };
    }

    private int readString20Length() {
        return ((15 & buffer.readUnsignedByte()) << 16)
                + (buffer.readUnsignedByte() << 8)
                + buffer.readUnsignedByte();
    }

    private String readStringFromToken(int token) {
        if (token < DICTIONARY_0.data() || token > DICTIONARY_3.data()) {
            return BinaryTokens.SINGLE_BYTE.get(token - 1);
        }

        var delta = (BinaryTokens.DOUBLE_BYTE.size() / 4) * (token - DICTIONARY_0.data());
        return BinaryTokens.DOUBLE_BYTE.get(buffer.readUnsignedByte() + delta);
    }

    private String readNibble() {
        var number = buffer.readUnsignedByte();
        return readString(BinaryTokens.NUMBERS, number >>> 7, 127 & number);
    }

    private Object readString(int size, boolean parseBytes) {
        var data = Buffers.readBytes(buffer, size);
        return parseBytes ? new String(data, StandardCharsets.UTF_8)
                : data;
    }

    private String readHexString() {
        var number = buffer.readUnsignedByte();
        return readString(BinaryTokens.HEX, number >>> 7, 127 & number);
    }

    private ContactJid readJidPair() {
        return switch (read(true)){
            case String encoded -> ContactJid.ofUser(encoded, forAddress(readString()));
            case null -> ContactJid.ofUser(null, forAddress(readString()));
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
        return LIST_8.contentEquals(token) ? buffer.readUnsignedByte()
                : buffer.readUnsignedShort();
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