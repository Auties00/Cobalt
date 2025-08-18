package it.auties.whatsapp.model.jid;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufString;
import it.auties.whatsapp.exception.MalformedJidException;
import it.auties.whatsapp.model.signal.session.SessionAddress;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A model class that represents a jid.
 * This class is only a model: this means that changing its values will have no real effect on WhatsappWeb's servers.
 */
public final class Jid implements JidProvider {
    private static final char PHONE_CHAR = '+';
    private static final char SERVER_CHAR = '@';
    private static final char DEVICE_CHAR = ':';
    private static final char AGENT_CHAR = '_';

    private static final Jid LEGACY_USER = new Jid(null, JidServer.legacyUser(), 0, 0);
    private static final Jid GROUP_OR_COMMUNITY = new Jid(null, JidServer.groupOrCommunity(), 0, 0);
    private static final Jid BROADCAST = new Jid(null, JidServer.broadcast(), 0, 0);
    private static final Jid CALL = new Jid(null, JidServer.call(), 0, 0);
    private static final Jid USER = new Jid(null, JidServer.user(), 0, 0);
    private static final Jid LID = new Jid(null, JidServer.lid(), 0, 0);
    private static final Jid NEWSLETTER = new Jid(null, JidServer.newsletter(), 0, 0);
    private static final Jid BOT = new Jid(null, JidServer.bot(), 0, 0);
    private static final ConcurrentMap<JidServer, Jid> unknownServerJidsStore = new ConcurrentHashMap<>();

    private static final Jid OFFICIAL_SURVEYS_ACCOUNT = new Jid("16505361212", JidServer.user(), 0, 0);
    private static final Jid OFFICIAL_BUSINESS_ACCOUNT = new Jid("16505361212", JidServer.legacyUser(), 0, 0);
    private static final Jid STATUS_BROADCAST = new Jid("status", JidServer.broadcast(), 0, 0);
    private static final Jid ANNOUNCEMENTS = new Jid("0", JidServer.user(), 0, 0);

    private final String user;
    private final JidServer server;
    private final int device;
    private final int agent;

    private Jid(String user, JidServer server, int device, int agent) {
        this.user = user;
        this.server = server;
        this.device = checkUnsignedByte(device);
        this.agent = checkUnsignedByte(agent);
    }

    private static int checkUnsignedByte(int i) {
        if(i < 0 || i > 255) {
            throw new MalformedJidException(i + " is not an unsigned byte");
        }
        return i;
    }

    public static Jid legacyUserServer() {
        return LEGACY_USER;
    }

    public static Jid groupOrCommunityServer() {
        return GROUP_OR_COMMUNITY;
    }

    public  static Jid broadcastServer() {
        return BROADCAST;
    }

    public static Jid callServer() {
        return CALL;
    }

    public static Jid userServer() {
        return USER;
    }

    public static Jid lidServer() {
        return LID;
    }

    public static Jid newsletterServer() {
        return NEWSLETTER;
    }

    public static Jid Server() {
        return BOT;
    }

    public static Jid officialSurveysAccount() {
        return OFFICIAL_SURVEYS_ACCOUNT;
    }

    public static Jid officialBusinessAccount() {
        return OFFICIAL_BUSINESS_ACCOUNT;
    }

    public static Jid statusBroadcastAccount() {
        return STATUS_BROADCAST;
    }

    public static Jid announcementsAccount() {
        return ANNOUNCEMENTS;
    }

    /**
     * Creates a new instance of Jid based on the provided input parameters.
     * If the user string starts with a '+' character, the character is removed before constructing the Jid.
     * Otherwise, the user string is used as is.
     *
     * @param user the user
     * @param server the non-null server
     * @param device the device
     * @param agent the agent
     * @return a non-null contact jid
     */
    public static Jid of(String user, JidServer server, int device, int agent) {
        Objects.requireNonNull(server, "Server cannot be null");
        if(user == null) {
            return new Jid(null, server, device, agent);
        }

        var inputLength = user.length();
        var offset = !user.isEmpty() && user.charAt(0) == PHONE_CHAR
                ? 1
                : 0;
        for(var i = 0; i < inputLength; i++) {
            var token = user.charAt(i);
            if (token == SERVER_CHAR || token == DEVICE_CHAR || token == AGENT_CHAR) {
                return new Jid(user.substring(offset, i), server, device, agent);
            }
        }
        return new Jid(offset == 0 ? user : user.substring(offset), server, device, agent);
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return switch (server.type()) {
            case UNKNOWN -> unknownServerJidsStore.computeIfAbsent(server, value -> new Jid(null, value, 0, 0));
            case LEGACY_USER -> LEGACY_USER;
            case GROUP_OR_COMMUNITY -> GROUP_OR_COMMUNITY;
            case BROADCAST -> BROADCAST;
            case CALL -> CALL;
            case USER -> USER;
            case LID -> LID;
            case NEWSLETTER -> NEWSLETTER;
            case BOT -> BOT;
        };
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    public static Jid of(long jid) {
        if(jid < 0) {
            throw new MalformedJidException("jid cannot be negative");
        }
        return new Jid(String.valueOf(jid), JidServer.user(), 0, 0);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the non-null jid
     * @return a non-null contact jid
     */
    public static Jid of(String jid) {
        if(jid == null) {
            return null;
        }

        var knownServer = JidServer.of(jid, false);
        if(knownServer != null) {
            return of(knownServer);
        }

        var serverSeparatorIndex = jid.indexOf(SERVER_CHAR);
        var server = serverSeparatorIndex == -1
                ? JidServer.user()
                : JidServer.of(jid, serverSeparatorIndex + 1, jid.length() - serverSeparatorIndex - 1);
        return parseJid(jid, serverSeparatorIndex, server);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param user    the nullable user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(String user, JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return parseJid(user, user.indexOf(SERVER_CHAR), server);
    }

    private static Jid parseJid(String jid, int jidLength, JidServer server) {
        var length = jidLength == -1 ? jid.length() : jidLength;
        if (length == 0) {
            return new Jid(null, server, 0, 0);
        }

        var offset = jid.charAt(0) == PHONE_CHAR ? 1 : 0;
        if (offset >= length) {
            throw new MalformedJidException("Malformed jid '" + jid + "'");
        }

        enum ParserState {
            USER,
            DEVICE,
            AGENT
        }

        var state = ParserState.USER;
        var userLength = length;
        var agent = 0;
        var device = 0;
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = jid.charAt(offset + parserPosition);
            if (token == SERVER_CHAR) {
                if(state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(jid, offset + parserPosition + 1, length - parserPosition - 1);
                break;
            }

            switch (state) {
                case USER -> {
                    if(token == DEVICE_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    }else if(token == AGENT_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if(agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if(Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    }else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if(device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if(Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    }else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                    }
                }
            }
        }
        var user = jid.substring(offset, offset + userLength);
        return new Jid(user, server, device, agent);
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    @ProtobufDeserializer
    public static Jid of(ProtobufString jid) {
        return switch(jid) {
            case ProtobufString.Lazy lazy -> Jid.of(lazy);
            case ProtobufString.Value value -> Jid.of(value.toString());
            case null -> null;
        };
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    public static Jid of(ProtobufString.Lazy jid) {
        if(jid == null) {
            return null;
        }

        enum ParserState {
            USER,
            DEVICE,
            AGENT,
            SERVER
        }

        var source = jid.encodedBytes();
        var offset = jid.encodedOffset();
        var length = jid.encodedLength();

        var knownServer = JidServer.of(source, offset, length, false);
        if(knownServer != null) {
            return of(knownServer);
        }

        var state = ParserState.USER;
        var userLength = length; // Do not allocate a char[], it's slower
        var agent = 0;
        var device = 0;
        var server = JidServer.user();
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = (char) (source[offset + parserPosition] & 0x7F);
            if (token == SERVER_CHAR) {
                if(state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(source, offset + parserPosition + 1, length - parserPosition - 1, true);
                break;
            }

            switch (state) {
                case USER -> {
                    if(token == DEVICE_CHAR) {
                        // device is already 0
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    }else if(token == AGENT_CHAR) {
                        // agent is already 0
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if(agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if(Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    }else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + value + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if(device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if(Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    }else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "'" + " while parsing jid '" + value + "'");
                    }
                }
            }
        }
        var user = new String(source, offset, userLength, StandardCharsets.UTF_8);
        return new Jid(user, server, device, agent);
    }

    /**
     * Returns whether this jid ends with the provided server
     *
     * @param server the server to check against
     * @return a boolean
     */
    public boolean hasServer(JidServer server) {
        return this.server.equals(server);
    }

    /**
     * Returns whether this jid is a server jid
     *
     * @param server the server to check against
     * @return a boolean
     */
    public boolean isServerJid(JidServer server) {
        return user == null && this.server.equals(server);
    }

    /**
     * Returns a new jid using with a different server
     *
     * @param server the new server
     * @return a non-null jid
     */
    public Jid withServer(JidServer server) {
        return Objects.equals(this.server, server)
                ? this
                : new Jid(user, server, device, agent);
    }

    /**
     * Returns a new jid using with a different agent
     *
     * @param agent the new agent
     * @return a non-null jid
     */
    public Jid withAgent(int agent) {
        return this.agent == agent
                ? this
                : new Jid(user, server, device, agent);
    }
    /**
     * Returns a new jid using with a different device
     *
     * @param device the new device
     * @return a non-null jid
     */
    public Jid withDevice(int device) {
        return this.device == device
                ? this
                : new Jid(user, server, device, agent);
    }

    /**
     * Converts this jid to a user jid
     *
     * @return a non-null jid
     */
    public Jid withoutData() {
        return !hasDevice() && !hasAgent()
                ? this
                : new Jid(user, server, 0, 0);
    }

    /**
     * Converts this jid to a non-formatted phone number
     *
     * @return a non-null String
     */
    public Optional<String> toPhoneNumber() {
        try {
            Long.parseLong(user);
            return Optional.of(PHONE_CHAR + user);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Converts this jid to a String
     *
     * @return a non-null String
     */
    @ProtobufSerializer
    @Override
    public String toString() {
        var hasUser = hasUser();
        var hasAgent = hasAgent();
        var hasDevice = hasDevice();
        if (!hasUser && !hasAgent && !hasDevice) {
            return server.toString();
        }

        var user = hasUser ? this.user : "";
        var agent = hasAgent ? "" + AGENT_CHAR + this.agent : "";
        var device = hasDevice ? "" + DEVICE_CHAR + this.device : "";
        return user + agent + device + SERVER_CHAR + server.toString();
    }

    /**
     * Converts this jid to a signal address
     *
     * @return a non-null {@link SessionAddress}
     */
    public SessionAddress toSignalAddress() {
        return new SessionAddress(user, device);
    }

    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    @Override
    public Jid toJid() {
        return this;
    }

    /**
     * Returns the user
     *
     * @return a nullable string
     */
    public String user() {
        return user;
    }

    /**
     * Returns the server
     *
     * @return a non-null server
     */
    public JidServer server() {
        return server;
    }

    /**
     * Returns the device
     *
     * @return an unsigned int
     */
    public int device() {
        return device;
    }

    /**
     * Returns the agent
     *
     * @return an unsigned int
     */
    public int agent() {
        return agent;
    }

    /**
     * Returns whether this jid specifies a user
     *
     * @return a boolean
     */
    public boolean hasUser() {
        return user != null;
    }

    /**
     * Returns whether this jid specifies a device
     *
     * @return a boolean
     */
    public boolean hasDevice() {
        return device != 0;
    }

    /**
     * Returns whether this jid specifies an agent
     *
     * @return a boolean
     */
    public boolean hasAgent() {
        return agent != 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Jid that
                && Objects.equals(user, that.user)
                && Objects.equals(server, that.server)
                && device == that.device
                && agent == that.agent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, server, device, agent);
    }
}