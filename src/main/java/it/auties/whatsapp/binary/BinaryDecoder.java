package it.auties.whatsapp.binary;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.utils.Buffers;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Inflater;

import static io.netty.buffer.ByteBufUtil.threadLocalDirectBuffer;
import static it.auties.whatsapp.binary.BinaryTag.*;

@AllArgsConstructor
public class BinaryDecoder{
    private ByteBuf binary;
    public BinaryDecoder(){
        this(Buffers.newBuffer());
    }
    
    public ByteBuf unpack(byte[] input) {
        var data = threadLocalDirectBuffer();
        data.writeBytes(input);
        var token = data.readUnsignedInt() & 2;
        if (token == 0) {
            return data.discardReadBytes();
        }

        try {
            var decompressor = new Inflater();
            decompressor.setInput(Buffers.readBytes(data.discardReadBytes()));
            var temp = new byte[2048];
            var length = decompressor.inflate(temp);
            var result = threadLocalDirectBuffer();
            result.writeBytes(Arrays.copyOf(temp, length));
            return result;
        }catch (Exception exception){
            throw new RuntimeException("Cannot inflate data", exception);
        }
    }

    public Node decode(ByteBuf binary){
        this.binary = binary;
        var token = binary.readUnsignedByte();
        var size = readListSize(token);
        if (size == 0) {
            throw new IllegalArgumentException("Failed to decode body: body cannot be empty");
        }

        var description = decodeString();
        var attrs = readAttributes(size);
        if (size % 2 != 0) {
            return new Node(description, attrs, null);
        }

        return new Node(description, attrs, read(false));
    }

    public String decodeString() {
        var read = read(true);
        if(read instanceof String string){
            return string;
        }

        throw new IllegalArgumentException("Strict decoding failed: expected string, got %s with type %s".formatted(read, read.getClass().getName()));
    }

    private List<Node> readList(int size){
        return IntStream.range(0, size)
                .mapToObj(index -> decode(binary))
                .toList();
    }

    private String readString(List<Character> permitted, int start, int end) {
        var string = new int[2 * end - start];
        IntStream.iterate(0, index -> index < string.length - 1, n -> n + 2)
                .forEach(index -> readChar(permitted, string, index));
        if (start != 0) {
            string[string.length - 1] = permitted.get(binary.readUnsignedByte() >>> 4);
        }

        return Arrays.stream(string)
                .mapToObj(e -> String.valueOf((char) e))
                .collect(Collectors.joining());
    }

    private void readChar(List<Character> permitted, int[] string, int index) {
        var token = binary.readUnsignedByte();
        string[index] = permitted.get(token >>> 4);
        string[index + 1] = permitted.get(15 & token);
    }

    private Object read(boolean parseBytes) {
        var tag = binary.readUnsignedByte();
        return switch (forData(tag)) {
            case LIST_EMPTY -> null;
            case COMPANION_JID -> readCompanionJid();
            case LIST_8 -> readList(binary.readUnsignedByte());
            case LIST_16 -> readList(binary.readUnsignedShort());
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
            return BinaryTokens.DOUBLE_BYTE.get(binary.readUnsignedByte() + delta);
        }

        return BinaryTokens.SINGLE_BYTE.get(token - 1);
    }

    private String readNibble() {
        var number = binary.readUnsignedByte();
        return readString(BinaryTokens.NUMBERS, number >>> 7, 127 & number);
    }

    private Object readString32(boolean parseBytes) {
        if (parseBytes) {
            var buffer = binary.readBytes(binary.readUnsignedShort());
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(binary, binary.readUnsignedShort());
    }

    private Object readString16(boolean parseBytes) {
        var size = ((15 & binary.readUnsignedByte()) << 16) + (binary.readUnsignedByte() << 8) + binary.readUnsignedByte();
        if (parseBytes) {
            var buffer = binary.readBytes(size);
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(binary, size);
    }

    private Object readString(boolean parseBytes) {
        var size = binary.readUnsignedByte();
        if (parseBytes) {
            var buffer = binary.readBytes(size);
            return new String(Buffers.readBytes(buffer), StandardCharsets.UTF_8);
        }

        return Buffers.readBytes(binary, size);
    }

    private String readHexString() {
        var number = binary.readUnsignedByte();
        return readString(BinaryTokens.HEX, number >>> 7, 127 & number);
    }

    private ContactId readJidPair() {
        return switch (read(true)){
            case String encoded -> ContactId.of(encoded, decodeString());
            case null -> ContactId.of(null, decodeString());
            default -> throw new RuntimeException("Invalid jid type");
        };
    }

    private ContactId readCompanionJid() {
        var agent = binary.readUnsignedByte();
        var device = binary.readUnsignedByte();
        var user = decodeString();
        return ContactId.ofCompanion(user, agent, device);
    }

    private int readListSize(int token) {
        if (token == LIST_8.data()){
            return binary.readUnsignedByte();
        }

        return binary.readUnsignedShort();
    }

    private Map<String, Object> readAttributes(int size) {
        var map = new HashMap<String, Object>();
        for (var t = size - 1; t > 1; t -= 2) {
            var key = decodeString();
            var value = read(true);
            map.put(key, value);
        }

        return map;
    }
}