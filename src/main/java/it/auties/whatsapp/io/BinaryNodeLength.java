package it.auties.whatsapp.io;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Strings;

import java.util.List;

import static it.auties.whatsapp.io.BinaryNodeTokens.*;

public final class BinaryNodeLength {
    private static final int UNSIGNED_BYTE_MAX_VALUE = 256;
    private static final int UNSIGNED_SHORT_MAX_VALUE = 65536;
    private static final int INT_20_MAX_VALUE = 1048576;

    public static int sizeOf(Node node) {
        return 1 + nodeLength(node);
    }

    private static int nodeLength(Node input){
        return listLength(input.size())
                + stringLength(input.description())
                + attributesLength(input.attributes())
                + (input.hasContent() ? contentLength(input.content()) : 0);
    }

    private static int listLength(int size) {
        if (size < UNSIGNED_BYTE_MAX_VALUE) {
            return 2;
        }else if (size < UNSIGNED_SHORT_MAX_VALUE) {
            return 3;
        }else {
            throw new IllegalArgumentException("Cannot calculate list length: overflow");
        }
    }

    private static int stringLength(String input){
        if (input.isEmpty()) {
            return 2;
        }

        var singleByteTokenIndex = SINGLE_BYTE_TOKENS.indexOf(input);
        if (singleByteTokenIndex != -1) {
            return 1;
        }

        var dictionary0TokenIndex = DICTIONARY_0_TOKENS.indexOf(input);
        if (dictionary0TokenIndex != -1) {
            return 2;
        }

        var dictionary1TokenIndex = DICTIONARY_1_TOKENS.indexOf(input);
        if (dictionary1TokenIndex != -1) {
            return 2;
        }

        var dictionary2TokenIndex = DICTIONARY_2_TOKENS.indexOf(input);
        if (dictionary2TokenIndex != -1) {
            return 2;
        }

        var dictionary3TokenIndex = DICTIONARY_3_TOKENS.indexOf(input);
        if (dictionary3TokenIndex != -1) {
            return 2;
        }

        var length = Strings.utf8Length(input);
        return binaryLength(length) + length;
    }

    private static int binaryLength(long input) {
        if (input < UNSIGNED_BYTE_MAX_VALUE) {
            return 2;
        }else if (input < INT_20_MAX_VALUE) {
            return 4;
        }else {
            return 5;
        }
    }

    private static int attributesLength(Attributes attributes) {
        return attributes.stream()
                .mapToInt(entry -> stringLength(entry.getKey()) + contentLength(entry.getValue()))
                .sum();
    }

    private static int childrenLength(List<?> values) {
        var length = listLength(values.size());
        for(var value : values) {
            try {
                length += nodeLength((Node) value);
            }catch (ClassCastException ignored) {
                throw new RuntimeException("Invalid payload type");
            }
        }
        return length;
    }

    private static int contentLength(Object input){
        return switch (input) {
            case null -> 1;
            case String value -> stringLength(value);
            case byte[] value -> bytesLength(value);
            case Boolean bool -> stringLength(Boolean.toString(bool));
            case Number number -> stringLength(number.toString());
            case Enum<?> value -> stringLength(value.toString());
            case Jid value -> jidLength(value);
            case List<?> value -> childrenLength(value);
            default -> throw new RuntimeException("Invalid payload type");
        };
    }

    private static int bytesLength(byte[] bytes){
        var length = bytes.length;
        return binaryLength(length) + length;
    }

    private static int jidLength(Jid jid){
        if (jid.isCompanion()) {
            return 3 + stringLength(jid.user());
        }else {
            return 2 + (jid.hasUser() ? stringLength(jid.user()) : 1);
        }
    }
}
