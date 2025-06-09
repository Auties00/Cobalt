package it.auties.whatsapp.util;

public final class Strings {
    public static int utf8Length(CharSequence sequence) {
        var count = 0;
        var len = sequence.length();
        for (var i = 0; i < len; i++) {
            var ch = sequence.charAt(i);
            if (ch <= 0x7F) {
                count++;
            } else if (ch <= 0x7FF) {
                count += 2;
            } else if (Character.isHighSurrogate(ch)) {
                count += 4;
                i++;
            } else {
                count += 3;
            }
        }
        return count;
    }
}
