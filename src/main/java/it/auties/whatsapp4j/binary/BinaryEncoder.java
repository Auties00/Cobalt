package it.auties.whatsapp4j.binary;

import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

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
    public byte @NonNull [] encodeMessage(@NonNull Node node) {
        cache.clear();
        return writeNode(node);
    }

    private byte @NonNull [] writeNode(@NonNull Node node) {
        writeListStart(2 * node.attrs().size() + 1 + (node.content() != null ? 1 : 0));
        writeString(node.description(), false);
        writeAttributes(node.attrs());
        writeContent(node.content());
        return toByteArray();
    }

    private void pushUnsignedInt(int value) {
        cache.add((byte) (value & 0xff));
    }

    private void pushInt4(int value) {
        IntStream.range(0, 4).map(i -> 3 - i).mapToObj(curShift -> (byte) ((value >> (curShift * 8)) & 0xff)).forEach(cache::add);
    }

    private void pushInt20(int value) {
        pushUnsignedInts(((value >> 16) & 0x0f), ((value >> 8) & 0xff), (value & 0xff));
    }

    private void pushUnsignedInts(int @NonNull ... ints) {
        Arrays.stream(ints).mapToObj(entry -> (byte) entry).forEachOrdered(cache::add);
    }

    private void pushString(@NonNull String str) {
        pushUnsignedInts(toUnsignedIntArray(str.getBytes()));
    }

    private void writeByteLength(int length) {
        var tag = length >= 1 << 20 ? BinaryTag.BINARY_32 : length >= 256 ? BinaryTag.BINARY_20 : BinaryTag.BINARY_8;
        pushUnsignedInt(tag.data());
        switch (tag) {
            case BINARY_32 -> pushInt4(length);
            case BINARY_20 -> pushInt20(length);
            case BINARY_8 -> pushUnsignedInt(length);
        }
    }

    private void writeStringRaw(@NonNull String string) {
        writeByteLength(string.length());
        pushString(string);
    }

    private void writeJid(String left, @NonNull String right) {
        pushUnsignedInt(BinaryTag.JID_PAIR.data());
        if (left != null && left.length() > 0) {
            writeStrings(left, right);
            return;
        }

        writeToken(BinaryTag.LIST_EMPTY.data());
        writeString(right, false);
    }

    private void writeToken(int token) {
        Validate.isTrue(token <= 500, "Invalid token");
        pushUnsignedInt(token);
    }

    private void writeString(@NonNull String token, boolean i) {
        var tokenIndex = BinaryTokens.SINGLE_BYTE.indexOf(token);
        if (!i && token.equals("s.whatsapp.net")) {
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
            writeStringRaw(token);
            return;
        }

        writeJid(token.substring(0, jidSepIndex), token.substring(jidSepIndex + 1));
    }

    private void writeStrings(@NonNull String... tokens) {
        Arrays.stream(tokens).forEach(token -> writeString(token, false));
    }

    private void writeAttributes(@NonNull Map<String, String> attrs) {
        attrs.forEach(this::writeStrings);
    }

    private void writeListStart(int listSize) {
        var tag = listSize == 0 ? BinaryTag.LIST_EMPTY : listSize < 256 ? BinaryTag.LIST_8 : BinaryTag.LIST_16;
        pushUnsignedInts(tag.data(), listSize);
    }

    @SneakyThrows
    private void writeContent(Object content) {
        if (content == null) {
            return;
        }

        if (content instanceof String contentAsString) {
            writeString(contentAsString, true);
            return;
        }

        if (content instanceof List<?> contentAsList) {
            Validate.isTrue(validateList(contentAsList), "Cannot encode content(%s): expected List<WhatsappNode>, got %s<?>", content, contentAsList.getClass().getTypeName());
            writeListStart(contentAsList.size());
            Node.fromGenericList(contentAsList).forEach(this::writeNode);
            return;
        }

        if (content instanceof MessageInfo contentAsMessage) {
            var data = ProtobufEncoder.encode(contentAsMessage);
            writeByteLength(data.length);
            pushUnsignedInts(toUnsignedIntArray(data));
            return;
        }

        throw new IllegalArgumentException("Cannot encode content " + content);
    }

    private boolean validateList(@NonNull List<?> list) {
        return list.stream().map(Object::getClass).allMatch(Node.class::isAssignableFrom);
    }

    private byte @NonNull [] toByteArray() {
        var array = new byte[cache.size()];
        IntStream.range(0, cache.size()).forEachOrdered(x -> array[x] = cache.get(x));
        return array;
    }

    private int @NonNull [] toUnsignedIntArray(byte @NonNull [] input) {
        return IntStream.range(0, input.length).map(x -> Byte.toUnsignedInt(input[x])).toArray();
    }
}