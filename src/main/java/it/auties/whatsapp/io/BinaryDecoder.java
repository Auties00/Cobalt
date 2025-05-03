package it.auties.whatsapp.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;

import javax.crypto.CipherInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.InflaterInputStream;

import static it.auties.whatsapp.io.BinaryTag.*;
import static it.auties.whatsapp.io.BinaryTokens.*;

public final class BinaryDecoder {
    private static final List<Character> NIBBLE_ALPHABET = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�', '�', '�');
    private static final List<Character> HEX_ALPHABET = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F');

    private final InputStream inputStream;

    private BinaryDecoder(InputStream inputStream) throws IOException {
        this.inputStream = (inputStream.read() & 2) == 0 ? inputStream : new InflaterInputStream(inputStream);
    }

    public static Node decode(InputStream stream) {
        try {
            var decoder = new BinaryDecoder(stream);
            return decoder.readNode();
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public Node readNode() throws IOException {
        var size = readNodeSize();
        // the description takes up one length unit
        var description = size-- > 0 ? readString() : null;
        // the attributes take up two length units for each entry(key and value pair)
        var attrs = readAttributes(size);
        // if the length, including the attributes but not the description, is odd then there is content
        var content = (size & 1) == 1 ? readContent() : null;
        return new Node(description, attrs, content);
    }

    private int readNodeSize() throws IOException {
        var token = (byte) inputStream.read();
        return switch (token) {
            case LIST_8 -> inputStream.read() & 0xFF;
            case LIST_16 -> (inputStream.read() << 8) | inputStream.read();
            default -> throw new IllegalStateException("Unexpected value: " + token);
        };
    }

    private String readString() throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case HEX_8 -> readPacked(HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8());
            case BINARY_20 -> new String(readBinary20());
            case BINARY_32 -> new String(readBinary32());
            case DICTIONARY_0 -> readDictionaryToken(DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    private byte[] readBinary8() throws IOException {
        var size = inputStream.read() & 0xFF;
        return inputStream.readNBytes(size);
    }

    private byte[] readBinary20() throws IOException {
        var size = (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    private byte[] readBinary32() throws IOException {
        var size = (inputStream.read() << 24)
                | (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    private String readDictionaryToken(BinaryTokens dictionary) throws IOException {
        var index = inputStream.read() & 0xFF;
        return dictionary.get(index);
    }

    private String readSingleByteToken(byte tag) {
        var index = tag & 0xFF;
        return SINGLE_BYTE_TOKENS.get(index);
    }

    private Attributes readAttributes(int size) throws IOException {
        var map = new LinkedHashMap<String, Object>();
        while (size >= 2) {
            var key = readString();
            var value = readAttribute();
            map.put(key, value);
            size -= 2;
        }
        return new Attributes(map);
    }

    private Object readAttribute() throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case COMPANION_JID -> readCompanionJid();
            case LIST_8 -> readList8();
            case LIST_16 -> readList16();
            case JID_PAIR -> readJidPair();
            case HEX_8 -> readPacked(HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8());
            case BINARY_20 -> new String(readBinary20());
            case BINARY_32 -> new String(readBinary32());
            case DICTIONARY_0 -> readDictionaryToken(DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    private List<Node> readList8() throws IOException {
        var length = inputStream.read() & 0xFF;
        return readList(length);
    }

    private List<Node> readList16() throws IOException {
        var length = (inputStream.read() << 8)
                | inputStream.read();
        return readList(length);
    }

    private List<Node> readList(int size) throws IOException {
        var results = new ArrayList<Node>(size);
        for (int index = 0; index < size; index++) {
            results.add(readNode());
        }
        return results;
    }

    private String readPacked(List<Character> alphabet) throws IOException {
        var token = inputStream.read() & 0xFF;
        var start = token >>> 7;
        var end = token & 127;
        var string = new char[2 * end - start];
        for(var index = 0; index < string.length - 1; index += 2) {
            token = inputStream.read() & 0xFF;
            string[index] = alphabet.get(token >>> 4);
            string[index + 1] = alphabet.get(15 & token);

        }
        if (start != 0) {
            token = inputStream.read() & 0xFF;
            string[string.length - 1] = alphabet.get(token >>> 4);
        }
        return String.valueOf(string);
    }

    private Jid readJidPair() throws IOException {
        var user = readString();
        var server = JidServer.of(Objects.requireNonNull(readString(), "Malformed jid pair: no server"));
        return user == null ? Jid.of(server) : Jid.of(user, server);
    }

    private Jid readCompanionJid() throws IOException {
        var agent = inputStream.read() & 0xFF;
        var device =  inputStream.read() & 0xFF;
        var user = readString();
        return Jid.of(user, JidServer.whatsapp(), device, agent);
    }

    private Object readContent() throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case COMPANION_JID -> readCompanionJid();
            case LIST_8 -> readList8();
            case LIST_16 -> readList16();
            case JID_PAIR -> readJidPair();
            case HEX_8 -> readPacked(HEX_ALPHABET);
            case BINARY_8 -> readBinary8();
            case BINARY_20 -> readBinary20();
            case BINARY_32 -> readBinary32();
            case NIBBLE_8 -> readPacked(NIBBLE_ALPHABET);
            case DICTIONARY_0 -> readDictionaryToken(DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    private static final class ByteBufferInputStream extends InputStream {
        private final ByteBuffer buffer;

        private ByteBufferInputStream(ByteBuffer buf) {
            this.buffer = buf;
        }

        @Override
        public int available() {
            return -1; // Don't need this
        }

        @Override
        public int read() {
            return this.buffer.hasRemaining() ? (this.buffer.get() & 0xFF) : -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) {
            if (!this.buffer.hasRemaining()) {
                return -1;
            } else {
                len = Math.min(len, this.buffer.remaining());
                this.buffer.get(bytes, off, len);
                return len;
            }
        }
    }
}
