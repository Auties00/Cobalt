package com.github.auties00.cobalt.wire.core.jid;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A representation of the server (domain) component of a WhatsApp JID.
 *
 * <p>Every WhatsApp JID contains a server component that identifies the domain to which the
 * entity belongs. The server appears after the {@code @} separator in the JID string
 * representation, for example {@code s.whatsapp.net} in the JID
 * {@code 1234567890@s.whatsapp.net}. Each known server domain is represented by a
 * pre-allocated singleton instance accessible through static factory methods, while
 * unrecognized domains are cached and reused through the {@link #unknown(String)} factory.
 *
 * <p>The known server domains used by WhatsApp Web are:
 *
 * <p>{@code s.whatsapp.net} is the current standard domain for user JIDs identified by
 * phone number. {@code c.us} is the legacy domain that served the same purpose before
 * the migration and is still recognized for backward compatibility. {@code g.us} is used
 * for group chat and community JIDs. {@code broadcast} is used for broadcast list JIDs
 * including the special Status broadcast. {@code call} is used for call-related JIDs.
 * {@code lid} is used for Linked Identity JIDs, which are opaque server-assigned
 * identifiers that act as an alternative to phone numbers. {@code newsletter} is used
 * for newsletter (Channel) JIDs. {@code bot} is used for bot JIDs such as Meta AI.
 * {@code hosted} and {@code hosted.lid} are used for business-hosted device JIDs.
 * {@code msgr} is used for Facebook Messenger interoperability JIDs. {@code interop}
 * is used for cross-platform interoperability JIDs.
 *
 * <p>Instances of this class are compared by their {@linkplain #address() address} string.
 * Two {@code JidServer} instances are considered equal if and only if they share the same
 * address, regardless of how they were obtained.
 *
 * @see Jid
 * @see Type
 */
public final class JidServer implements JidProvider {
    /**
     * The address string for the legacy user server domain.
     */
    private static final String LEGACY_USER_ADDRESS = "c.us";

    /**
     * The address string for the group or community server domain.
     */
    private static final String GROUP_OR_COMMUNITY_ADDRESS = "g.us";

    /**
     * The address string for the broadcast server domain.
     */
    private static final String BROADCAST_ADDRESS = "broadcast";

    /**
     * The address string for the call server domain.
     */
    private static final String CALL_ADDRESS = "call";

    /**
     * The address string for the current standard user server domain.
     */
    private static final String USER_ADDRESS = "s.whatsapp.net";

    /**
     * The address string for the Linked Identity server domain.
     */
    private static final String LID_ADDRESS = "lid";

    /**
     * The address string for the newsletter server domain.
     */
    private static final String NEWSLETTER_ADDRESS = "newsletter";

    /**
     * The address string for the bot server domain.
     */
    private static final String BOT_ADDRESS = "bot";

    /**
     * The address string for the business-hosted server domain.
     */
    private static final String HOSTED_ADDRESS = "hosted";

    /**
     * The address string for the business-hosted LID server domain.
     */
    private static final String HOSTED_LID_ADDRESS = "hosted.lid";

    /**
     * The address string for the Facebook Messenger interoperability server domain.
     */
    private static final String MESSENGER_ADDRESS = "msgr";

    /**
     * The address string for the cross-platform interoperability server domain.
     */
    private static final String INTEROP_ADDRESS = "interop";

    /**
     * The singleton instance for the legacy user server ({@code c.us}).
     */
    private static final JidServer LEGACY_USER = new JidServer(LEGACY_USER_ADDRESS, Type.LEGACY_USER);

    /**
     * The singleton instance for the group or community server ({@code g.us}).
     */
    private static final JidServer GROUP_OR_COMMUNITY = new JidServer(GROUP_OR_COMMUNITY_ADDRESS, Type.GROUP_OR_COMMUNITY);

    /**
     * The singleton instance for the broadcast server ({@code broadcast}).
     */
    private static final JidServer BROADCAST = new JidServer(BROADCAST_ADDRESS, Type.BROADCAST);

    /**
     * The singleton instance for the call server ({@code call}).
     */
    private static final JidServer CALL = new JidServer(CALL_ADDRESS, Type.CALL);

    /**
     * The singleton instance for the current standard user server ({@code s.whatsapp.net}).
     */
    private static final JidServer USER = new JidServer(USER_ADDRESS, Type.USER);

    /**
     * The singleton instance for the Linked Identity server ({@code lid}).
     */
    private static final JidServer LID = new JidServer(LID_ADDRESS, Type.LID);

    /**
     * The singleton instance for the newsletter server ({@code newsletter}).
     */
    private static final JidServer NEWSLETTER = new JidServer(NEWSLETTER_ADDRESS, Type.NEWSLETTER);

    /**
     * The singleton instance for the bot server ({@code bot}).
     */
    private static final JidServer BOT = new JidServer(BOT_ADDRESS, Type.BOT);

    /**
     * The singleton instance for the business-hosted server ({@code hosted}).
     */
    private static final JidServer HOSTED = new JidServer(HOSTED_ADDRESS, Type.HOSTED);

    /**
     * The singleton instance for the business-hosted LID server ({@code hosted.lid}).
     */
    private static final JidServer HOSTED_LID = new JidServer(HOSTED_LID_ADDRESS, Type.HOSTED_LID);

    /**
     * The singleton instance for the Facebook Messenger interoperability server ({@code msgr}).
     */
    private static final JidServer MESSENGER = new JidServer(MESSENGER_ADDRESS, Type.MSGR);

    /**
     * The singleton instance for the cross-platform interoperability server ({@code interop}).
     */
    private static final JidServer INTEROP = new JidServer(INTEROP_ADDRESS, Type.INTEROP);

    /**
     * A concurrent cache of {@code JidServer} instances for unrecognized server domains.
     */
    private static final ConcurrentMap<String, JidServer> unknownServersStore = new ConcurrentHashMap<>();

    /**
     * The raw address string for this server domain, such as {@code "s.whatsapp.net"} or
     * {@code "g.us"}.
     */
    final String address;

    /**
     * The classified type of this server domain, or {@link Type#UNKNOWN} if the domain
     * does not match any known WhatsApp server.
     */
    final Type type;

    /**
     * Constructs a new {@code JidServer} with the specified address and type.
     *
     * @param address the raw server domain string
     * @param type    the classified type of this server
     */
    JidServer(String address, Type type) {
        this.address = address;
        this.type = type;
    }

    /**
     * Returns the singleton instance for the legacy user server domain {@code c.us}.
     *
     * <p>This domain was historically used for WhatsApp user JIDs before the platform
     * migrated to {@code s.whatsapp.net}. It is still recognized for backward
     * compatibility when parsing JIDs from older protocol messages.
     *
     * @return the legacy user server instance
     */
    public static JidServer legacyUser() {
        return LEGACY_USER;
    }

    /**
     * Returns the singleton instance for the group and community server domain {@code g.us}.
     *
     * <p>Group JIDs in this domain typically encode the creator's phone number and a
     * creation timestamp as the user component, for example
     * {@code 1234567890-1609459200@g.us}.
     *
     * @return the group or community server instance
     */
    public static JidServer groupOrCommunity() {
        return GROUP_OR_COMMUNITY;
    }

    /**
     * Returns the singleton instance for the broadcast server domain {@code broadcast}.
     *
     * <p>This domain is used for broadcast list JIDs, including the special
     * {@code status@broadcast} JID for WhatsApp Status updates and
     * {@code location@broadcast} for live location sharing broadcasts.
     *
     * @return the broadcast server instance
     */
    public  static JidServer broadcast() {
        return BROADCAST;
    }

    /**
     * Returns the singleton instance for the call server domain {@code call}.
     *
     * <p>Call JIDs in this domain use hexadecimal identifiers. The bare string
     * {@code "call"} (without an {@code @} prefix) also represents the call link
     * domain in WhatsApp Web.
     *
     * @return the call server instance
     */
    public static JidServer call() {
        return CALL;
    }

    /**
     * Returns the singleton instance for the current standard user server domain
     * {@code s.whatsapp.net}.
     *
     * <p>This is the modern domain for WhatsApp user JIDs identified by phone number.
     * User JIDs follow the pattern {@code phonenumber@s.whatsapp.net}. Device JIDs
     * extend this with agent and device identifiers, for example
     * {@code 1234567890_1:2@s.whatsapp.net}.
     *
     * @return the user server instance
     */
    public static JidServer user() {
        return USER;
    }

    /**
     * Returns the singleton instance for the Linked Identity server domain {@code lid}.
     *
     * <p>LID (Linked Identity) JIDs use opaque numeric identifiers assigned by the
     * server as an alternative to phone numbers. They provide privacy by not exposing
     * the user's phone number in protocol messages.
     *
     * @return the LID server instance
     */
    public static JidServer lid() {
        return LID;
    }

    /**
     * Returns the singleton instance for the newsletter server domain {@code newsletter}.
     *
     * <p>Newsletters (also known as Channels) are one-to-many broadcast channels that
     * allow administrators to publish content to subscribers. Newsletter JIDs use
     * numeric identifiers in this domain.
     *
     * @return the newsletter server instance
     */
    public static JidServer newsletter() {
        return NEWSLETTER;
    }

    /**
     * Returns the singleton instance for the bot server domain {@code bot}.
     *
     * <p>Bot JIDs in this domain represent AI assistants and automated agents, including
     * Meta AI. Bot JIDs use numeric identifiers assigned by the platform.
     *
     * @return the bot server instance
     */
    public static JidServer bot() {
        return BOT;
    }

    /**
     * Returns the singleton instance for the business-hosted server domain {@code hosted}.
     *
     * <p>Hosted device JIDs represent devices managed by a business solution provider.
     * These JIDs always use device identifier {@code 99} and map to the
     * {@code s.whatsapp.net} domain when extracting the underlying user JID.
     *
     * @return the hosted server instance
     */
    public static JidServer hosted() {
        return HOSTED;
    }

    /**
     * Returns the singleton instance for the business-hosted LID server domain
     * {@code hosted.lid}.
     *
     * <p>Similar to the {@linkplain #hosted() hosted} domain, but for devices that use
     * Linked Identity addressing instead of phone numbers. The device identifier is
     * always {@code 99}. These JIDs map to the {@code lid} domain when extracting the
     * underlying user JID.
     *
     * @return the hosted LID server instance
     */
    public static JidServer hostedLid() {
        return HOSTED_LID;
    }

    /**
     * Returns the singleton instance for the Facebook Messenger interoperability server
     * domain {@code msgr}.
     *
     * <p>Messenger JIDs represent users and devices from Facebook Messenger participating
     * in cross-platform conversations with WhatsApp users. Messenger user JIDs use
     * numeric identifiers in this domain.
     *
     * @return the Messenger server instance
     */
    public static JidServer messenger() {
        return MESSENGER;
    }

    /**
     * Returns the singleton instance for the cross-platform interoperability server
     * domain {@code interop}.
     *
     * <p>Interop JIDs represent users and devices from external platforms communicating
     * with WhatsApp through broader cross-platform interoperability protocols. Interop
     * user identifiers follow the pattern {@code countrycode-number}.
     *
     * @return the interop server instance
     */
    public static JidServer interop() {
        return INTEROP;
    }

    /**
     * Returns a cached {@code JidServer} instance for an unrecognized server domain.
     *
     * <p>If a {@code JidServer} for the given address has already been created, the
     * previously cached instance is returned. Otherwise a new instance with type
     * {@link Type#UNKNOWN} is created, cached, and returned.
     *
     * @param address the raw server domain string
     * @return a {@code JidServer} with type {@code UNKNOWN} for the given address
     */
    public static JidServer unknown(String address) {
        return unknownServersStore.computeIfAbsent(address, value -> new JidServer(value, Type.UNKNOWN));
    }

    /**
     * Returns the {@code JidServer} instance corresponding to the given address string.
     *
     * <p>If the address matches a known WhatsApp server domain, the corresponding
     * singleton is returned. Otherwise a cached {@link Type#UNKNOWN} instance is
     * created and returned.
     *
     * @param address the raw server domain string
     * @return the matching {@code JidServer} instance, never {@code null}
     */
    public static JidServer of(String address) {
        return of(address, true);
    }

    /**
     * Returns the {@code JidServer} instance corresponding to the given address string,
     * optionally returning {@code null} for unrecognized domains.
     *
     * @param address      the raw server domain string
     * @param allowUnknown if {@code true}, unrecognized domains produce a cached
     *                     {@link Type#UNKNOWN} instance; if {@code false}, they
     *                     produce {@code null}
     * @return the matching {@code JidServer}, or {@code null} if the domain is
     *         unrecognized and {@code allowUnknown} is {@code false}
     */
    static JidServer of(String address, boolean allowUnknown) {
        return switch (address) {
            case LEGACY_USER_ADDRESS -> LEGACY_USER;
            case GROUP_OR_COMMUNITY_ADDRESS -> GROUP_OR_COMMUNITY;
            case BROADCAST_ADDRESS -> BROADCAST;
            case CALL_ADDRESS -> CALL;
            case USER_ADDRESS -> USER;
            case LID_ADDRESS -> LID;
            case NEWSLETTER_ADDRESS -> NEWSLETTER;
            case BOT_ADDRESS -> BOT;
            case HOSTED_ADDRESS -> HOSTED;
            case HOSTED_LID_ADDRESS -> HOSTED_LID;
            case MESSENGER_ADDRESS -> MESSENGER;
            case INTEROP_ADDRESS -> INTEROP;
            default -> allowUnknown ? unknown(address) : null;
        };
    }

    /**
     * Returns the {@code JidServer} instance by matching characters from the given
     * string at the specified offset and length.
     *
     * <p>This is a fast-path parser that avoids substring allocation by comparing
     * individual characters in place. If no known domain matches, a substring is
     * allocated and passed to {@link #unknown(String)}.
     *
     * @param address the source string containing the server domain
     * @param offset  the starting index of the domain within the source string
     * @param length  the number of characters that compose the domain
     * @return the matching {@code JidServer} instance, never {@code null}
     */
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
                    case 'm' -> {
                        if (address.charAt(offset + 1) == 's'
                            && address.charAt(offset + 2) == 'g'
                            && address.charAt(offset + 3) == 'r') {
                            return MESSENGER;
                        }
                    }
                }
            }
            case 6 -> {
                if (address.charAt(offset) == 'h'
                    && address.charAt(offset + 1) == 'o'
                    && address.charAt(offset + 2) == 's'
                    && address.charAt(offset + 3) == 't'
                    && address.charAt(offset + 4) == 'e'
                    && address.charAt(offset + 5) == 'd') {
                    return HOSTED;
                }
            }
            case 7 -> {
                if (address.charAt(offset) == 'i'
                    && address.charAt(offset + 1) == 'n'
                    && address.charAt(offset + 2) == 't'
                    && address.charAt(offset + 3) == 'e'
                    && address.charAt(offset + 4) == 'r'
                    && address.charAt(offset + 5) == 'o'
                    && address.charAt(offset + 6) == 'p') {
                    return INTEROP;
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
                switch (address.charAt(offset)) {
                    case 'n' -> {
                        if (address.charAt(offset + 1) == 'e'
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
                    case 'h' -> {
                        if (address.charAt(offset + 1) == 'o'
                            && address.charAt(offset + 2) == 's'
                            && address.charAt(offset + 3) == 't'
                            && address.charAt(offset + 4) == 'e'
                            && address.charAt(offset + 5) == 'd'
                            && address.charAt(offset + 6) == '.'
                            && address.charAt(offset + 7) == 'l'
                            && address.charAt(offset + 8) == 'i'
                            && address.charAt(offset + 9) == 'd') {
                            return HOSTED_LID;
                        }
                    }
                }
            }
            case 14 -> {
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
        return unknown(offset == 0 && address.length() == length
                ? address
                : address.substring(offset, offset + length));
    }

    /**
     * Returns the {@code JidServer} instance by matching bytes from the given array
     * at the specified offset and length.
     *
     * <p>This is a fast-path parser optimized for raw byte arrays, typically originating
     * from protobuf lazy string representations. Each byte is masked with {@code 0x7F}
     * to handle high-bit encoding. If no known domain matches and {@code allowUnknown}
     * is {@code true}, the bytes are decoded as US-ASCII and passed to
     * {@link #unknown(String)}.
     *
     * @param source       the byte array containing the server domain
     * @param offset       the starting index of the domain within the byte array
     * @param length       the number of bytes that compose the domain
     * @param allowUnknown if {@code true}, unrecognized domains produce a cached
     *                     {@link Type#UNKNOWN} instance; if {@code false}, they
     *                     produce {@code null}
     * @return the matching {@code JidServer}, or {@code null} if the domain is
     *         unrecognized and {@code allowUnknown} is {@code false}
     */
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
                    case 'm' -> {
                        if ((char) (source[offset + 1] & 0x7F) == 's'
                            && (char) (source[offset + 2] & 0x7F) == 'g'
                            && (char) (source[offset + 3] & 0x7F) == 'r') {
                            return MESSENGER;
                        }
                    }
                }
            }
            case 6 -> {
                if ((char) (source[offset] & 0x7F) == 'h'
                    && (char) (source[offset + 1] & 0x7F) == 'o'
                    && (char) (source[offset + 2] & 0x7F) == 's'
                    && (char) (source[offset + 3] & 0x7F) == 't'
                    && (char) (source[offset + 4] & 0x7F) == 'e'
                    && (char) (source[offset + 5] & 0x7F) == 'd') {
                    return HOSTED;
                }
            }
            case 7 -> {
                if ((char) (source[offset] & 0x7F) == 'i'
                    && (char) (source[offset + 1] & 0x7F) == 'n'
                    && (char) (source[offset + 2] & 0x7F) == 't'
                    && (char) (source[offset + 3] & 0x7F) == 'e'
                    && (char) (source[offset + 4] & 0x7F) == 'r'
                    && (char) (source[offset + 5] & 0x7F) == 'o'
                    && (char) (source[offset + 6] & 0x7F) == 'p') {
                    return INTEROP;
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
                switch ((char) (source[offset] & 0x7F)) {
                    case 'n' -> {
                        if ((char) (source[offset + 1] & 0x7F) == 'e'
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
                    case 'h' -> {
                        if ((char) (source[offset + 1] & 0x7F) == 'o'
                            && (char) (source[offset + 2] & 0x7F) == 's'
                            && (char) (source[offset + 3] & 0x7F) == 't'
                            && (char) (source[offset + 4] & 0x7F) == 'e'
                            && (char) (source[offset + 5] & 0x7F) == 'd'
                            && (char) (source[offset + 6] & 0x7F) == '.'
                            && (char) (source[offset + 7] & 0x7F) == 'l'
                            && (char) (source[offset + 8] & 0x7F) == 'i'
                            && (char) (source[offset + 9] & 0x7F) == 'd') {
                            return HOSTED_LID;
                        }
                    }
                }
            }
            case 14 -> {
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

    /**
     * Returns the raw address string for this server domain.
     *
     * @return the address string, such as {@code "s.whatsapp.net"} or {@code "g.us"}
     */
    public String address() {
        return address;
    }

    /**
     * Returns a {@link Jid} that represents this server domain without a user component.
     *
     * @return a non-null server-only {@code Jid}
     */
    @Override
    public Jid toJid() {
        return Jid.of(this);
    }

    /**
     * Returns the raw address string for this server domain.
     *
     * @return the address string, identical to {@link #address()}
     */
    @Override
    public String toString() {
        return address;
    }

    /**
     * Compares this server to the specified object for equality.
     *
     * <p>Two {@code JidServer} instances are considered equal if and only if they have
     * the same {@linkplain #address() address} string.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code JidServer} with the same
     *         address, {@code false} otherwise
     */
    @Override
    public boolean equals(Object other) {
        return this == other
               || other instanceof JidServer that && Objects.equals(address, that.address);
    }

    /**
     * Returns a hash code for this server based on its {@linkplain #address() address}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    /**
     * Returns the classified {@link Type} of this server domain.
     *
     * @return the server type, or {@link Type#UNKNOWN} for unrecognized domains
     */
    public Type type() {
        return type;
    }

    /**
     * An enumeration of the known WhatsApp server domain types.
     *
     * <p>Each constant in this enum corresponds to one of the recognized server domains
     * used in WhatsApp JIDs, as defined by the WhatsApp Web client. The
     * {@link #UNKNOWN} constant is used as a fallback for domains that do not match any
     * known type.
     */
    public enum Type {
        /**
         * A server domain type that does not match any known WhatsApp server domain.
         * Instances with this type are created for unrecognized domains encountered
         * during JID parsing.
         */
        UNKNOWN,

        /**
         * The legacy server domain type for the {@code c.us} domain, historically used
         * for WhatsApp user JIDs before the migration to {@code s.whatsapp.net}.
         */
        LEGACY_USER,

        /**
         * The server domain type for the {@code g.us} domain, used for group chat and
         * community JIDs.
         */
        GROUP_OR_COMMUNITY,

        /**
         * The server domain type for the {@code broadcast} domain, used for broadcast
         * list JIDs including the Status broadcast.
         */
        BROADCAST,

        /**
         * The server domain type for the {@code call} domain, used for call-related
         * JIDs with hexadecimal identifiers.
         */
        CALL,

        /**
         * The server domain type for the {@code s.whatsapp.net} domain, the current
         * standard domain for phone-number-based user JIDs.
         */
        USER,

        /**
         * The server domain type for the {@code lid} domain, used for Linked Identity
         * JIDs that provide privacy by replacing phone numbers with opaque numeric
         * identifiers.
         */
        LID,

        /**
         * The server domain type for the {@code newsletter} domain, used for newsletter
         * (Channel) JIDs.
         */
        NEWSLETTER,

        /**
         * The server domain type for the {@code bot} domain, used for AI assistant and
         * automated agent JIDs.
         */
        BOT,

        /**
         * The server domain type for the {@code hosted} domain, used for business-hosted
         * device JIDs that always carry device identifier {@code 99}.
         */
        HOSTED,

        /**
         * The server domain type for the {@code hosted.lid} domain, used for
         * business-hosted device JIDs with Linked Identity addressing that always carry
         * device identifier {@code 99}.
         */
        HOSTED_LID,

        /**
         * The server domain type for the {@code msgr} domain, used for Facebook Messenger
         * interoperability JIDs.
         */
        MSGR,

        /**
         * The server domain type for the {@code interop} domain, used for cross-platform
         * interoperability JIDs.
         */
        INTEROP
    }
}
