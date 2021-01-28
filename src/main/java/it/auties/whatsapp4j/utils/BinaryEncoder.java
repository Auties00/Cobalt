/*
    Getting this class to work was not very easy, but I surely couldn't have done it without the help of:
    https://github.com/JicuNull/WhatsJava/blob/master/src/main/java/icu/jnet/whatsjava/encryption/BinaryEncoder.java - Java implementation, helped me to correctly cast a byte to an unsigned int, before I was using a method that just didn't work
    https://github.com/adiwajshing/Baileys/blob/master/src/Binary/Encoder.ts - Typescript implementation, the logic was far less error prone than the one used by the python implementation on https://github.com/sigalor/whatsapp-web-reveng and the one I came up with.
 */
package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.constant.Tag;
import it.auties.whatsapp4j.constant.Tokens;
import it.auties.whatsapp4j.model.WhatsappNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public record BinaryEncoder(List<Byte> cache) {
    public byte @NotNull [] encodeMessage(@NotNull WhatsappNode node){
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
        pushUnsignedInts(new int[]{((value >> 16) & 0x0f), ((value >> 8) & 0xff), (value & 0xff)});
    }

    private void pushUnsignedInts(int @NotNull [] ints) {
        for(var entry : ints) cache.add((byte) entry);
    }

    private void pushString(@NotNull String str) {
        pushUnsignedInts(toUnsignedIntArray(str.getBytes()));
    }

    private void writeByteLength(int length) {
        if (length >= 1 << 20) {
            this.pushUnsignedInt(Tag.BINARY_32.data());
            this.pushInt4(length);
        } else if (length >= 256) {
            this.pushUnsignedInt(Tag.BINARY_20.data());
            this.pushInt20(length);
        } else {
            this.pushUnsignedInt(Tag.BINARY_8.data());
            this.pushUnsignedInt(length);
        }
    }

    private void writeStringRaw(@NotNull String string) {
        this.writeByteLength(string.length());
        this.pushString(string);
    }

    private void writeJid(@Nullable String left, @NotNull String right) {
        this.pushUnsignedInt(Tag.JID_PAIR.data());
        if(left != null && left.length() > 0){
            writeString(left, false);
        } else {
            this.writeToken(Tag.LIST_EMPTY.data());
        }

        this.writeString(right, false);
    }

    private void writeToken(int token) {
        Validate.isTrue(token <= 500, "Invalid token");
        this.pushUnsignedInt(token);
    }

    private void writeString(@NotNull String token, boolean i) {
        var tokenIndex = Tokens.SINGLE_BYTE_TOKENS.indexOf(token);
        if (!i && token.equals("s.whatsapp.net")) {
            this.writeToken(tokenIndex);
        } else if (tokenIndex >= 0) {
            if (tokenIndex < Tag.SINGLE_BYTE_MAX.data()) {
                this.writeToken(tokenIndex);
            } else {
                var overflow = tokenIndex - Tag.SINGLE_BYTE_MAX.data();
                var dictionaryIndex = overflow >> 8;
                Validate.isTrue(dictionaryIndex >= 0 && dictionaryIndex <= 3, "Token out of range!");
                this.writeToken(Tag.DICTIONARY_0.data() + dictionaryIndex);
                this.writeToken(overflow % 256);
            }
        } else {
            var jidSepIndex = token.indexOf('@');
            if (jidSepIndex <= 0) {
                this.writeStringRaw(token);
            } else {
                this.writeJid(token.substring(0, jidSepIndex), token.substring(jidSepIndex + 1));
            }
        }
    }

    private void writeAttributes(@NotNull Map<String, String> attrs) {
        attrs.forEach((key, value) -> {
                this.writeString(key, false);
                this.writeString(value, false);
        });
    }

    private void writeListStart(int listSize) {
        if (listSize == 0) {
            this.pushUnsignedInt(Tag.LIST_EMPTY.data());
        } else if (listSize < 256) {
            this.pushUnsignedInts(new int[]{Tag.LIST_8.data(), listSize});
        } else {
            this.pushUnsignedInts(new int[]{Tag.LIST_16.data(), listSize});
        }
    }

    private void writeContent(@Nullable Object content) {
        if(content == null){
            return;
        }

        if(content instanceof String contentAsString){
            writeString(contentAsString, true);
        }else if(content instanceof List<?> contentAsList){
            Validate.isTrue(contentAsList.stream().allMatch(e -> e instanceof WhatsappNode), "Cannot encode content(%s): expected List<WhatsappNode>, got List<%s>", content, contentAsList.getClass().getTypeName());
            writeListStart(contentAsList.size());
            contentAsList.stream().map(WhatsappNode.class::cast).filter(Objects::nonNull).forEach(this::writeNode);
        }else if(content instanceof ProtoBuf.WebMessageInfo contentAsMessage){
            var data = contentAsMessage.toByteArray();
            this.writeByteLength(data.length);
            this.pushUnsignedInts(toUnsignedIntArray(data));
        }else {
            throw new IllegalArgumentException("Cannot encode content " + content);
        }
    }

    private byte @NotNull [] toByteArray(){
        var array = new byte[cache.size()];
        for (var x = 0; x < cache.size(); x++) array[x] = cache.get(x);
        return array;
    }

    private int @NotNull [] toUnsignedIntArray(byte @NotNull [] input){
        var array = new int[input.length];
        for (var x = 0; x < input.length; x++) array[x] = input[x] & 0xff;
        return array;
    }
}