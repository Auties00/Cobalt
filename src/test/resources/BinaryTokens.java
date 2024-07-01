package it.auties.whatsapp.io;

import it.auties.whatsapp.model.companion.CompanionProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class BinaryTokens {
    public static final List<String> SINGLE_BYTE = List.of(%s);

    public static final List<String> DOUBLE_BYTE = List.of(%s);

    public static final int DICTIONARY_VERSION = 3;

    public static final List<Character> NUMBERS = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�', '�', '�');

    public static final List<Character> HEX = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F');

    public static final String NUMBERS_REGEX = "[^0-9.-]+?";

    public static final String HEX_REGEX = "[^0-9A-F]+?";

    public static final Map<Integer, CompanionProperty> PROPERTIES;

    static {
        var properties = new HashMap<Integer, CompanionProperty>();
%s
        //noinspection Java9CollectionFactory
        PROPERTIES = Collections.unmodifiableMap(properties);
    }

    public static boolean anyMatch(String input, String regex) {
        return Pattern.compile(regex)
                .matcher(input)
                .results()
                .findAny()
                .isPresent();
    }
}
