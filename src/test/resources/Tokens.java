package it.auties.whatsapp.binary;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class Tokens {
    public final List<String> SINGLE_BYTE = List.of( % s);

    public final List<String> DOUBLE_BYTE = List.of( % s);

    public final List<Character> NUMBERS = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '�', '�',
            '�', '�');

    public final List<Character> HEX = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F');

    public final String NUMBERS_REGEX = "[^0-9.-]+?";

    public final String HEX_REGEX = "[^0-9A-F]+?";

    public boolean noMatch(@NonNull String input, @NonNull String regex) {
        return Pattern.compile(regex)
                .matcher(input)
                .results()
                .findAny()
                .isEmpty();
    }
}
