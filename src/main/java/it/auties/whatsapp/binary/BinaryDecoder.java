package it.auties.whatsapp.binary;

import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.model.Node;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Inflater;

import static it.auties.whatsapp.binary.BinaryTag.*;

@AllArgsConstructor
public class BinaryDecoder{
    private BinaryBuffer binary;
    public BinaryDecoder(){
        this(new BinaryBuffer());
    }
    public BinaryBuffer unpack(byte[] input) {
        var data = BinaryBuffer.of(input);
        var token = data.readUInt8() & 2;
        if (token == 0) {
            return data.remaining();
        }

        try {
            var decompressor = new Inflater();
            decompressor.setInput(data.remaining().readAllBytes());
            var temp = new byte[2048];
            var length = decompressor.inflate(temp);
            var result = new byte[length];
            System.arraycopy(temp, 0, result, 0, length);
            return BinaryBuffer.of(result);
        }catch (Exception exception){
            throw new RuntimeException("Cannot inflate data", exception);
        }
    }

    public Node decode(BinaryBuffer binary){
        this.binary = binary;
        var token = binary.readUInt8();
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

    private Object read(boolean parseBytes) {
        var tag = binary.readUInt8();
        return switch (forData(tag)) {
            case LIST_EMPTY -> null;
            case AD_JID -> readAdJid();
            case LIST_8 -> readList(binary.readUInt8());
            case LIST_16 -> readList(binary.readUInt16());
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
            return BinaryTokens.DOUBLE_BYTE.get(binary.readUInt8() + delta);
        }

        return BinaryTokens.SINGLE_BYTE.get(token - 1);
    }

    private String readNibble() {
        var number = binary.readUInt8();
        return readString(BinaryTokens.NUMBERS, number >>> 7, 127 & number);
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
        return readString(BinaryTokens.HEX, number >>> 7, 127 & number);
    }

    private ContactId readJidPair() {
        return switch (read(true)){
            case String encoded -> ContactId.create(encoded, decodeString());
            case null -> ContactId.create(null, decodeString());
            default -> throw new RuntimeException("Invalid jid type");
        };
    }

    private ContactId readAdJid() {
        var agent = binary.readUInt8();
        var device = binary.readUInt8();
        var user = decodeString();
        return ContactId.createAd(user, agent, device);
    }

    private int readListSize(int token) {
        if (token == LIST_8.data()){
            return binary.readUInt8();
        }

        return binary.readUInt16();
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