package it.auties.whatsapp.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Bytes;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.auties.whatsapp.io.BinaryTag.*;

public final class BinaryDecoder implements AutoCloseable {
    private final DataInputStream dataInputStream;
    private boolean closed;
    public BinaryDecoder(byte[] buffer) {
        var token = buffer[0] & 2;
        if (token == 0) {
            this.dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer, 1, buffer.length - 1));
        }else {
            this.dataInputStream = new DataInputStream(new ByteArrayInputStream(Bytes.decompress(buffer, 1, buffer.length - 1)));
        }
    }
    
    public Node decode() throws IOException {
        if(closed) {
            throw new IllegalStateException("The encoder is closed");
        }

        var token = dataInputStream.readUnsignedByte();
        var size = readSize(token);
        if (size == 0) {
            throw new IllegalArgumentException("Cannot decode node with empty body");
        }
        var description = readString();
        var attrs = readAttributes(size);
        return size % 2 != 0 ? Node.of(description, attrs) : Node.of(description, attrs, read(false));
    }

    private String readString() throws IOException {
        var read = read(true);
        if (read instanceof String string) {
            return string;
        }

        throw new IllegalArgumentException("Strict decoding failed: expected string, got %s with type %s"
                .formatted(read, read == null ? null : read.getClass().getName()));
    }

    private List<Node> readList(int size) throws IOException {
        var results = new ArrayList<Node>();
        for (int index = 0; index < size; index++) {
            results.add(decode());
        }
        
        return results;
    }

    private String readString(List<Character> permitted, int start, int end) throws IOException {
        var string = new char[2 * end - start];
        for(var index = 0; index < string.length - 1; index += 2) {
            readChar(permitted, string, index);
        }
        if (start != 0) {
            string[string.length - 1] = permitted.get(dataInputStream.readUnsignedByte() >>> 4);
        }

        return String.valueOf(string);
    }

    private void readChar(List<Character> permitted, char[] string, int index) throws IOException {
        var token = dataInputStream.readUnsignedByte();
        string[index] = permitted.get(token >>> 4);
        string[index + 1] = permitted.get(15 & token);
    }

    private Object read(boolean parseBytes) throws IOException {
        var tag = dataInputStream.readUnsignedByte();
        return switch (of(tag)) {
            case LIST_EMPTY -> null;
            case COMPANION_JID -> readCompanionJid();
            case LIST_8 -> readList(dataInputStream.readUnsignedByte());
            case LIST_16 -> readList(dataInputStream.readUnsignedShort());
            case JID_PAIR -> readJidPair();
            case HEX_8 -> readHexString();
            case BINARY_8 -> readString(dataInputStream.readUnsignedByte(), parseBytes);
            case BINARY_20 -> readString(readString20Length(), parseBytes);
            case BINARY_32 -> readString(dataInputStream.readUnsignedShort(), parseBytes);
            case NIBBLE_8 -> readNibble();
            default -> readStringFromToken(tag);
        };
    }

    private int readString20Length() throws IOException {
        return ((15 & dataInputStream.readUnsignedByte()) << 16)
                + ((dataInputStream.readUnsignedByte()) << 8)
                + (dataInputStream.readUnsignedByte());
    }

    private String readStringFromToken(int token) throws IOException {
        if (token < DICTIONARY_0.data() || token > DICTIONARY_3.data()) {
            return BinaryTokens.SINGLE_BYTE.get(token - 1);
        }

        var delta = (BinaryTokens.DOUBLE_BYTE.size() / 4) * (token - DICTIONARY_0.data());
        return BinaryTokens.DOUBLE_BYTE.get(dataInputStream.readUnsignedByte() + delta);
    }

    private String readNibble() throws IOException {
        var number = dataInputStream.readUnsignedByte();
        return readString(BinaryTokens.NUMBERS, number >>> 7, 127 & number);
    }

    private Object readString(int size, boolean parseBytes) throws IOException {
        var data = new byte[size];
        dataInputStream.readFully(data);
        return parseBytes ? new String(data, StandardCharsets.UTF_8) : data;
    }

    private String readHexString() throws IOException {
        var number = dataInputStream.readUnsignedByte();
        return readString(BinaryTokens.HEX, number >>> 7, 127 & number);
    }

    private Jid readJidPair() throws IOException {
        return switch (read(true)) {
            case String encoded -> Jid.of(encoded, JidServer.of(readString()));
            case null -> Jid.of(JidServer.of(readString()));
            default -> throw new RuntimeException("Invalid jid type");
        };
    }

    private Jid readCompanionJid() throws IOException {
        var agent = dataInputStream.readUnsignedByte();
        var device = dataInputStream.readUnsignedByte();
        var user = readString();
        return Jid.of(user, JidServer.whatsapp(), device, agent);
    }

    private int readSize(int token) throws IOException {
        return LIST_8.contentEquals(token) ? dataInputStream.readUnsignedByte() : dataInputStream.readUnsignedShort();
    }

    private Map<String, Object> readAttributes(int size) throws IOException {
        var map = new HashMap<String, Object>();
        for (var pair = size - 1; pair > 1; pair -= 2) {
            var key = readString();
            var value = read(true);
            map.put(key, getValueWithContext(key, value));
        }
        return map;
    }

    private static Object getValueWithContext(String key, Object value) {
        if (value instanceof Jid jid && Objects.equals(key, "lid")) {
            return jid.withServer(JidServer.lid());
        }

        return value;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        dataInputStream.close();
    }
}
