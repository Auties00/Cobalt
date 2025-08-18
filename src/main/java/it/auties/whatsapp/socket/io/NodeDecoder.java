package it.auties.whatsapp.socket.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.InflaterInputStream;

import static it.auties.whatsapp.socket.io.NodeTags.*;
import static it.auties.whatsapp.socket.io.NodeTokens.*;

public final class NodeDecoder {
    private static final List<Character> NIBBLE_ALPHABET = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�', '�', '�');
    private static final List<Character> HEX_ALPHABET = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F');

    private NodeDecoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Node decode(InputStream stream) throws IOException {
        if((stream.read() & 2) == 0) {
            return readNode(stream);
        }else {
            return readNode(new InflaterInputStream(stream));
        }
    }

    private static Node readNode(InputStream inputStream) throws IOException {
        var size = readNodeSize(inputStream);
        // the description takes up one length unit
        var description = size-- > 0 ? readString(inputStream) : null;
        // the attributes take up two length units for each entry(key and value pair)
        var attrs = readAttributes(inputStream, size);
        // if the length, including the attributes but not the description, is odd then there is content
        var content = (size & 1) == 1 ? readContent(inputStream) : null;
        return new Node(description, attrs, content);
    }

    private static int readNodeSize(InputStream inputStream) throws IOException {
        var token = (byte) inputStream.read();
        return switch (token) {
            case LIST_8 -> inputStream.read() & 0xFF;
            case LIST_16 -> (inputStream.read() << 8) | inputStream.read();
            default -> throw new IllegalStateException("Unexpected value: " + token);
        };
    }

    private static String readString(InputStream inputStream) throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case HEX_8 -> readPacked(inputStream, HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(inputStream, NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8(inputStream));
            case BINARY_20 -> new String(readBinary20(inputStream));
            case BINARY_32 -> new String(readBinary32(inputStream));
            case DICTIONARY_0 -> readDictionaryToken(inputStream, DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(inputStream, DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(inputStream, DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(inputStream, DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    private static byte[] readBinary8(InputStream inputStream) throws IOException {
        var size = inputStream.read() & 0xFF;
        return inputStream.readNBytes(size);
    }

    private static byte[] readBinary20(InputStream inputStream) throws IOException {
        var size = (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    private static byte[] readBinary32(InputStream inputStream) throws IOException {
        var size = (inputStream.read() << 24)
                | (inputStream.read() << 16)
                | (inputStream.read() << 8)
                | inputStream.read();
        return inputStream.readNBytes(size);
    }

    private static String readDictionaryToken(InputStream inputStream, NodeTokens dictionary) throws IOException {
        var index = inputStream.read() & 0xFF;
        return dictionary.get(index);
    }

    private static String readSingleByteToken(byte tag) {
        var index = tag & 0xFF;
        return SINGLE_BYTE_TOKENS.get(index);
    }

    private static Attributes readAttributes(InputStream inputStream, int size) throws IOException {
        var map = new LinkedHashMap<String, Object>();
        while (size >= 2) {
            var key = readString(inputStream);
            var value = readAttribute(inputStream);
            map.put(key, value);
            size -= 2;
        }
        return new Attributes(map);
    }

    private static Object readAttribute(InputStream inputStream) throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case AD_JID -> readAdJid(inputStream);
            case LIST_8 -> readList8(inputStream);
            case LIST_16 -> readList16(inputStream);
            case JID_PAIR -> readJidPair(inputStream);
            case HEX_8 -> readPacked(inputStream, HEX_ALPHABET);
            case NIBBLE_8 -> readPacked(inputStream, NIBBLE_ALPHABET);
            case BINARY_8 -> new String(readBinary8(inputStream));
            case BINARY_20 -> new String(readBinary20(inputStream));
            case BINARY_32 -> new String(readBinary32(inputStream));
            case DICTIONARY_0 -> readDictionaryToken(inputStream, DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(inputStream, DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(inputStream, DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(inputStream, DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }

    private static List<Node> readList8(InputStream inputStream) throws IOException {
        var length = inputStream.read() & 0xFF;
        return readList(inputStream, length);
    }

    private static List<Node> readList16(InputStream inputStream) throws IOException {
        var length = (inputStream.read() << 8)
                | inputStream.read();
        return readList(inputStream, length);
    }

    private static List<Node> readList(InputStream inputStream, int size) throws IOException {
        var results = new ArrayList<Node>(size);
        for (int index = 0; index < size; index++) {
            results.add(readNode(inputStream));
        }
        return results;
    }

    private static String readPacked(InputStream inputStream, List<Character> alphabet) throws IOException {
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

    private static Jid readJidPair(InputStream inputStream) throws IOException {
        var user = readString(inputStream);
        var server = JidServer.of(Objects.requireNonNull(readString(inputStream), "Malformed jid pair: no server"));
        return user == null ? Jid.of(server) : Jid.of(user, server);
    }

    private static Jid readAdJid(InputStream inputStream) throws IOException {
        var agent = inputStream.read() & 0xFF;
        var device = inputStream.read() & 0xFF;
        var user = readString(inputStream);
        return Jid.of(user, JidServer.user(), device, agent);
    }

    private static Object readContent(InputStream inputStream) throws IOException {
        var tag = (byte) inputStream.read();
        return switch (tag) {
            case LIST_EMPTY -> null;
            case AD_JID -> readAdJid(inputStream);
            case LIST_8 -> readList8(inputStream);
            case LIST_16 -> readList16(inputStream);
            case JID_PAIR -> readJidPair(inputStream);
            case HEX_8 -> readPacked(inputStream, HEX_ALPHABET);
            case BINARY_8 -> readBinary8(inputStream);
            case BINARY_20 -> readBinary20(inputStream);
            case BINARY_32 -> readBinary32(inputStream);
            case NIBBLE_8 -> readPacked(inputStream, NIBBLE_ALPHABET);
            case DICTIONARY_0 -> readDictionaryToken(inputStream, DICTIONARY_0_TOKENS);
            case DICTIONARY_1 -> readDictionaryToken(inputStream, DICTIONARY_1_TOKENS);
            case DICTIONARY_2 -> readDictionaryToken(inputStream, DICTIONARY_2_TOKENS);
            case DICTIONARY_3 -> readDictionaryToken(inputStream, DICTIONARY_3_TOKENS);
            default -> readSingleByteToken(tag);
        };
    }
}