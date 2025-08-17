package it.auties.whatsapp.model.jid;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The constants of this enumerated type describe the various servers that a jid might be linked
 * to
 */
public final class JidServer implements JidProvider { // String parsing is hard part 2
    private static final String LEGACY_USER_ADDRESS = "c.us";
    private static final String GROUP_OR_COMMUNITY_ADDRESS = "g.us";
    private static final String BROADCAST_ADDRESS = "broadcast";
    private static final String CALL_ADDRESS = "call";
    private static final String USER_ADDRESS = "s.whatsapp.net";
    private static final String LID_ADDRESS = "lid";
    private static final String NEWSLETTER_ADDRESS = "newsletter";
    private static final String BOT_ADDRESS = "bot";

    private static final JidServer LEGACY_USER = new JidServer(LEGACY_USER_ADDRESS, Type.LEGACY_USER);
    private static final JidServer GROUP_OR_COMMUNITY = new JidServer(GROUP_OR_COMMUNITY_ADDRESS, Type.GROUP_OR_COMMUNITY);
    private static final JidServer BROADCAST = new JidServer(BROADCAST_ADDRESS, Type.BROADCAST);
    private static final JidServer CALL = new JidServer(CALL_ADDRESS, Type.CALL);
    private static final JidServer USER = new JidServer(USER_ADDRESS, Type.USER);
    private static final JidServer LID = new JidServer(LID_ADDRESS, Type.LID);
    private static final JidServer NEWSLETTER = new JidServer(NEWSLETTER_ADDRESS, Type.NEWSLETTER);
    private static final JidServer BOT = new JidServer(BOT_ADDRESS, Type.BOT);
    private static final ConcurrentMap<String, JidServer> unknownServersStore = new ConcurrentHashMap<>();

    private final String address;
    private final Type type;
    private JidServer(String address, Type type) {
        this.address = address;
        this.type = type;
    }

    public static JidServer legacyUser() {
        return LEGACY_USER;
    }

    public static JidServer groupOrCommunity() {
        return GROUP_OR_COMMUNITY;
    }

    public  static JidServer broadcast() {
        return BROADCAST;
    }

    public static JidServer call() {
        return CALL;
    }

    public static JidServer user() {
        return USER;
    }

    public static JidServer lid() {
        return LID;
    }

    public static JidServer newsletter() {
        return NEWSLETTER;
    }

    public static JidServer bot() {
        return BOT;
    }

    public static JidServer unknown(String address) {
        return unknownServersStore.computeIfAbsent(address, value -> new JidServer(value, Type.UNKNOWN));
    }

    public static JidServer of(String address) {
        return of(address, true);
    }

    public static JidServer of(String address, boolean allowUnknown) {
        return switch (address) {
            case LEGACY_USER_ADDRESS -> LEGACY_USER;
            case GROUP_OR_COMMUNITY_ADDRESS -> GROUP_OR_COMMUNITY;
            case BROADCAST_ADDRESS -> BROADCAST;
            case CALL_ADDRESS -> CALL;
            case USER_ADDRESS -> USER;
            case LID_ADDRESS -> LID;
            case NEWSLETTER_ADDRESS -> NEWSLETTER;
            case BOT_ADDRESS -> BOT;
            default -> allowUnknown ? unknown(address) : null;
        };
    }

    // Fast path
    static JidServer of(String address, int offset, int length) {
        if(length == 0) {
            return USER;
        }

        switch (length) {
            case 3 -> {
                switch (address.charAt(offset)) {
                    case 'l' -> {
                        if (address.charAt(offset + 1) == 'i'
                                && address.charAt(offset + 2) == 'd') {
                            return LID;
                        }
                    }
                    case 'b' -> {
                        if (address.charAt(offset + 1) == 'o'
                                && address.charAt(offset + 2) == 't') {
                            return BOT;
                        }
                    }
                }
            }
            case 4 -> {
                switch (address.charAt(offset)) {
                    case 'c' -> {
                        switch (address.charAt(offset + 1)) {
                            case '.' -> {
                                if (address.charAt(offset + 2) == 'u'
                                        && address.charAt(offset + 3) == 's') {
                                    return LEGACY_USER;
                                }
                            }
                            case 'a' -> {
                                if (address.charAt(offset + 2) == 'l'
                                        && address.charAt(offset + 3) == 'l') {
                                    return CALL;
                                }
                            }
                        }
                    }
                    case 'g' -> {
                        if (address.charAt(offset + 1) == '.'
                                && address.charAt(offset + 2) == 'u'
                                && address.charAt(offset + 3) == 's') {
                            return GROUP_OR_COMMUNITY;
                        }
                    }
                }
            }
            case 9 -> {
                if (address.charAt(offset) == 'b'
                        && address.charAt(offset + 1) == 'r'
                        && address.charAt(offset + 2) == 'o'
                        && address.charAt(offset + 3) == 'a'
                        && address.charAt(offset + 4) == 'd'
                        && address.charAt(offset + 5) == 'c'
                        && address.charAt(offset + 6) == 'a'
                        && address.charAt(offset + 7) == 's'
                        && address.charAt(offset + 8) == 't') {
                    return BROADCAST;
                }
            }
            case 10 -> {
                if (address.charAt(offset) == 'n'
                        && address.charAt(offset + 1) == 'e'
                        && address.charAt(offset + 2) == 'w'
                        && address.charAt(offset + 3) == 's'
                        && address.charAt(offset + 4) == 'l'
                        && address.charAt(offset + 5) == 'e'
                        && address.charAt(offset + 6) == 't'
                        && address.charAt(offset + 7) == 't'
                        && address.charAt(offset + 8) == 'e'
                        && address.charAt(offset + 9) == 'r') {
                    return NEWSLETTER;
                }
            }
            case 13 -> {
                if (address.charAt(offset) == 's'
                        && address.charAt(offset + 1) == '.'
                        && address.charAt(offset + 2) == 'w'
                        && address.charAt(offset + 3) == 'h'
                        && address.charAt(offset + 4) == 'a'
                        && address.charAt(offset + 5) == 't'
                        && address.charAt(offset + 6) == 's'
                        && address.charAt(offset + 7) == 'a'
                        && address.charAt(offset + 8) == 'p'
                        && address.charAt(offset + 9) == 'p'
                        && address.charAt(offset + 10) == '.'
                        && address.charAt(offset + 11) == 'n'
                        && address.charAt(offset + 12) == 'e'
                        && address.charAt(offset + 13) == 't') {
                    return USER;
                }
            }
        }
        return unknown(offset == 0 ? address : address.substring(offset));
    }

    // Fast path
    static JidServer of(byte[] source, int offset, int length, boolean allowUnknown) {
        if (length == 0) {
            return USER;
        }

        switch (length) {
            case 3 -> {
                switch ((char) (source[offset] & 0x7F)) {
                    case 'l' -> {
                        if ((char) (source[offset + 1] & 0x7F) == 'i'
                                && (char) (source[offset + 2] & 0x7F) == 'd') {
                            return LID;
                        }
                    }
                    case 'b' -> {
                        if ((char) (source[offset + 1] & 0x7F) == 'o'
                                && (char) (source[offset + 2] & 0x7F) == 't') {
                            return BOT;
                        }
                    }
                }
            }
            case 4 -> {
                switch ((char) (source[offset] & 0x7F)) {
                    case 'c' -> {
                        switch ((char) (source[offset + 1] & 0x7F)) {
                            case '.' -> {
                                if ((char) (source[offset + 2] & 0x7F) == 'u'
                                        && (char) (source[offset + 3] & 0x7F) == 's') {
                                    return LEGACY_USER;
                                }
                            }
                            case 'a' -> {
                                if ((char) (source[offset + 2] & 0x7F) == 'l'
                                        && (char) (source[offset + 3] & 0x7F) == 'l') {
                                    return CALL;
                                }
                            }
                        }
                    }
                    case 'g' -> {
                        if ((char) (source[offset + 1] & 0x7F) == '.'
                                && (char) (source[offset + 2] & 0x7F) == 'u'
                                && (char) (source[offset + 3] & 0x7F) == 's') {
                            return GROUP_OR_COMMUNITY;
                        }
                    }
                }
            }
            case 9 -> {
                if ((char) (source[offset] & 0x7F) == 'b'
                        && (char) (source[offset + 1] & 0x7F) == 'r'
                        && (char) (source[offset + 2] & 0x7F) == 'o'
                        && (char) (source[offset + 3] & 0x7F) == 'a'
                        && (char) (source[offset + 4] & 0x7F) == 'd'
                        && (char) (source[offset + 5] & 0x7F) == 'c'
                        && (char) (source[offset + 6] & 0x7F) == 'a'
                        && (char) (source[offset + 7] & 0x7F) == 's'
                        && (char) (source[offset + 8] & 0x7F) == 't') {
                    return BROADCAST;
                }
            }
            case 10 -> {
                if ((char) (source[offset] & 0x7F) == 'n'
                        && (char) (source[offset + 1] & 0x7F) == 'e'
                        && (char) (source[offset + 2] & 0x7F) == 'w'
                        && (char) (source[offset + 3] & 0x7F) == 's'
                        && (char) (source[offset + 4] & 0x7F) == 'l'
                        && (char) (source[offset + 5] & 0x7F) == 'e'
                        && (char) (source[offset + 6] & 0x7F) == 't'
                        && (char) (source[offset + 7] & 0x7F) == 't'
                        && (char) (source[offset + 8] & 0x7F) == 'e'
                        && (char) (source[offset + 9] & 0x7F) == 'r') {
                    return NEWSLETTER;
                }
            }
            case 13 -> {
                if ((char) (source[offset] & 0x7F) == 's'
                        && (char) (source[offset + 1] & 0x7F) == '.'
                        && (char) (source[offset + 2] & 0x7F) == 'w'
                        && (char) (source[offset + 3] & 0x7F) == 'h'
                        && (char) (source[offset + 4] & 0x7F) == 'a'
                        && (char) (source[offset + 5] & 0x7F) == 't'
                        && (char) (source[offset + 6] & 0x7F) == 's'
                        && (char) (source[offset + 7] & 0x7F) == 'a'
                        && (char) (source[offset + 8] & 0x7F) == 'p'
                        && (char) (source[offset + 9] & 0x7F) == 'p'
                        && (char) (source[offset + 10] & 0x7F) == '.'
                        && (char) (source[offset + 11] & 0x7F) == 'n'
                        && (char) (source[offset + 12] & 0x7F) == 'e'
                        && (char) (source[offset + 13] & 0x7F) == 't') {
                    return USER;
                }
            }
        }
        return allowUnknown
                ? unknown(new String(source, offset, length, StandardCharsets.US_ASCII))
                : null;
    }

    public String address() {
        return address;
    }

    @Override
    public Jid toJid() {
        return Jid.of(this);
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof JidServer that && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, type);
    }

    public Type type() {
        return type;
    }

    public enum Type {
        UNKNOWN,
        LEGACY_USER,
        GROUP_OR_COMMUNITY,
        BROADCAST,
        CALL,
        USER,
        LID,
        NEWSLETTER,
        BOT
    }
}
