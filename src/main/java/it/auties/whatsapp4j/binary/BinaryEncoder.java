package it.auties.whatsapp4j.binary;

import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.validation.constraints.NotNull;

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
public record BinaryEncoder(@NotNull List<Byte> cache) {

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
    public byte @NotNull [] encodeMessage(@NotNull WhatsappNode node) {
        cache.clear();
        return writeNode(node);
    }

    private byte @NotNull [] writeNode(@NotNull WhatsappNode node) {
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

    private void pushUnsignedInts(int @NotNull ... ints) {
        Arrays.stream(ints).mapToObj(entry -> (byte) entry).forEachOrdered(cache::add);
    }

    private void pushString(@NotNull String str) {
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

    private void writeStringRaw(@NotNull String string) {
        writeByteLength(string.length());
        pushString(string);
    }

    private void writeJid(String left, @NotNull String right) {
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

    private void writeString(@NotNull String token, boolean i) {
        var tokenIndex = BinaryTokens.SINGLE_BYTE.indexOf(token);
        if (!i && token.equals("s.org.example.whatsapp.net")) {
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

    private void writeStrings(@NotNull String... tokens) {
        Arrays.stream(tokens).forEach(token -> writeString(token, false));
    }

    private void writeAttributes(@NotNull Map<String, String> attrs) {
        attrs.forEach(this::writeStrings);
    }

    private void writeListStart(int listSize) {
        var tag = listSize == 0 ? BinaryTag.LIST_EMPTY : listSize < 256 ? BinaryTag.LIST_8 : BinaryTag.LIST_16;
        pushUnsignedInts(tag.data(), listSize);
    }

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
            WhatsappNode.fromGenericList(contentAsList).forEach(this::writeNode);
            return;
        }

        if (content instanceof WhatsappProtobuf.WebMessageInfo contentAsMessage) {
            var data = contentAsMessage.toByteArray();
            writeByteLength(data.length);
            pushUnsignedInts(toUnsignedIntArray(data));
            return;
        }

        throw new IllegalArgumentException("Cannot encode content " + content);
    }

    private boolean validateList(@NotNull List<?> list) {
        return list.stream().map(Object::getClass).allMatch(WhatsappNode.class::isAssignableFrom);
    }

    private byte @NotNull [] toByteArray() {
        var array = new byte[cache.size()];
        IntStream.range(0, cache.size()).forEachOrdered(x -> array[x] = cache.get(x));
        return array;
    }

    private int @NotNull [] toUnsignedIntArray(byte @NotNull [] input) {
        return IntStream.range(0, input.length).map(x -> Byte.toUnsignedInt(input[x])).toArray();
    }
}