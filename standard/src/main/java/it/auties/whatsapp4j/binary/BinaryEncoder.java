package it.auties.whatsapp4j.binary;

import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp4j.common.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.utils.Nodes;
import it.auties.whatsapp4j.common.utils.Validate;
import it.auties.whatsapp4j.common.binary.BinaryTag;
import lombok.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A class used to encode a WhatsappNode and then send it to WhatsappWeb's WebSocket.
 * To decode a message use instead {@link BinaryDecoder}.
 *
 * @param cache the message to encode
 */
public record BinaryEncoder(@NonNull List<Byte> cache) {
    /**
     * Constructs a new empty {@link BinaryEncoder}
     */
    public BinaryEncoder() {
        this(new ArrayList<>());
    }

    /**
     * Encodes {@code node} as an array of bytes
     *
     * @return a new array of bytes
     */
    public byte @NonNull [] encode(@NonNull Node node) {
        cache.clear();
        return writeNode(node);
    }

    private byte @NonNull [] writeNode(@NonNull Node node) {
        writeListStart(node.size());
        writeString(node.description(), false);
        writeAttributes(node.attrs());
        writeContent(node.content());
        return toByteArray();
    }

    private void push(byte... bytes) {
        IntStream.range(0, bytes.length)
                .mapToObj(index -> (byte) Byte.toUnsignedInt(bytes[index]))
                .forEach(cache::add);
    }

    private void push(int... ints) {
        Arrays.stream(ints)
                .mapToObj(anInt -> (byte) anInt)
                .forEach(cache::add);
    }

    private void pushInt(int value) {
        IntStream.iterate(3, x -> x > 0, x -> x - 1)
                .mapToObj(curShift -> value >> (curShift * 8))
                .forEach(this::push);
    }

    private void pushInt20(int value) {
        var header = (value >> 16) & 0x0f;
        var part = (value >> 8) & 0xff;
        var trailing = value & 0xff;
        push(header, part, trailing);
    }

    private void pushString(@NonNull String str) {
        push(str.getBytes());
    }

    private void writeByteLength(int length) {
        var tag = length >= 1 << 20 ? BinaryTag.BINARY_32 : length >= 256 ? BinaryTag.BINARY_20 : BinaryTag.BINARY_8;
        push(tag.data());
        switch (tag) {
            case BINARY_32 -> pushInt(length);
            case BINARY_20 -> pushInt20(length);
            case BINARY_8 -> push(length);
        }
    }

    private void writeString(@NonNull String string) {
        writeByteLength(string.length());
        pushString(string);
    }

    private void writeJid(String left, @NonNull String right) {
        push(BinaryTag.JID_PAIR.data());
        if (left != null && left.length() > 0) {
            writeStrings(left, right);
            return;
        }

        writeToken(BinaryTag.LIST_EMPTY.data());
        writeString(right, false);
    }

    private void writeToken(int token) {
        Validate.isTrue(token <= 500, "Invalid token");
        push(token);
    }

    private void writeString(@NonNull String token, boolean content) {
        var tokenIndex = BinaryTokens.SINGLE_BYTE.indexOf(token);
        if (!content && token.equals("s.whatsapp.net")) {
            writeToken(tokenIndex);
            return;
        }

        if (tokenIndex >= 0) {
            if (tokenIndex < BinaryTag.SINGLE_BYTE_MAX.data()) {
                writeToken(tokenIndex);
                return;
            }

            var overflow = tokenIndex - BinaryTag.SINGLE_BYTE_MAX.data();
            var dictionaryIndex = overflow >> 8;
            Validate.isTrue(dictionaryIndex >= 0 && dictionaryIndex <= 3, "Token out of range!");
            writeToken(BinaryTag.DICTIONARY_0.data() + dictionaryIndex);
            writeToken(overflow % 256);
            return;
        }

        var jidSepIndex = token.indexOf('@');
        if (jidSepIndex <= 0) {
            writeString(token);
            return;
        }

        writeJid(token.substring(0, jidSepIndex), token.substring(jidSepIndex + 1));
    }

    private void writeStrings(@NonNull String... tokens) {
        Arrays.stream(tokens).forEach(token -> writeString(token, false));
    }

    private void writeAttributes(@NonNull Map<String, Object> attrs) {
        attrs.forEach(this::writeStrings);
    }

    private void writeListStart(int listSize) {
        var tag = listSize == 0 ? BinaryTag.LIST_EMPTY : listSize < 256 ? BinaryTag.LIST_8 : BinaryTag.LIST_16;
        push(tag.data(), listSize);
    }


    private void writeContent(Object content) {
        switch (content){
            case null -> {}
            case String contentAsString -> writeString(contentAsString, true);
            case List<?> contentAsList -> writeList(contentAsList);
            case MessageInfo contentAsMessage -> writeMessage(contentAsMessage);
            default -> throw new IllegalArgumentException("Cannot encode content %s of type %s".formatted(content, content.getClass().getName()));
        }
    }

    private void writeList(List<?> contentAsList) {
        var parsedList = Nodes.validNodes(contentAsList);
        Validate.isTrue(contentAsList.size() != parsedList.size(), "Cannot encode content: expected List<WhatsappNode>, got %s<?>", contentAsList.getClass().getTypeName());
        writeListStart(parsedList.size());
        parsedList.forEach(this::writeNode);
    }

    private void writeMessage(MessageInfo contentAsMessage) {
        try {
            var data = ProtobufEncoder.encode(contentAsMessage);
            writeByteLength(data.length);
            push(data);
        }catch (IOException | IllegalAccessException exception){
            throw new RuntimeException("Cannot encode message: %s".formatted(contentAsMessage), exception);
        }
    }

    private byte @NonNull [] toByteArray() {
        var array = new byte[cache.size()];
        IntStream.range(0, cache.size()).forEachOrdered(x -> array[x] = cache.get(x));
        return array;
    }
}