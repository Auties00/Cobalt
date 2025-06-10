package it.auties.whatsapp.model.jid;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufString;
import it.auties.whatsapp.model.signal.session.SessionAddress;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a jid.
 * This class is only a model: this means that changing its values will have no real effect on WhatsappWeb's servers.
 */
@Json
public final class Jid implements JidProvider { // How can string parsing get so complicated?
    final String user;
    final JidServer server;
    final int device;
    final int agent;

    private Jid(String user, JidServer server, int device, int agent) {
        this.user = user;
        this.server = server;
        this.device = device;
        this.agent = agent;
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
        if (user != null && !user.isEmpty() && user.charAt(0) == '+') {
            user = user.substring(1);
        }
        return new Jid(user, server, device, agent);
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return new Jid(null, server, 0, 0);
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    public static Jid of(long jid) {
        return new Jid(String.valueOf(jid), JidServer.whatsapp(), 0, 0);
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
            case ProtobufString.Lazy lazy -> {
                var source = lazy.encodedBytes();
                var offset = lazy.encodedOffset();
                var length = lazy.encodedLength();
                var splitOffset = length;
                var state = ParserState.USER;
                userLengthLabel: {
                    for(var position = 0; position < length; position++) {
                        switch (getAsciiChar(source, offset + position)) {
                            case '@' -> {
                                splitOffset = position;
                                state = ParserState.SERVER;
                                break userLengthLabel;
                            }
                            case ':' -> {
                                splitOffset = position;
                                state = ParserState.DEVICE;
                                break userLengthLabel;
                            }
                            case '_' -> {
                                splitOffset = position;
                                state = ParserState.AGENT;
                                break userLengthLabel;
                            }
                        }
                    }
                }
                yield switch (state) {
                    case USER -> parseJid(source, offset, splitOffset);
                    case SERVER -> parseJid(source, offset, length, splitOffset);
                    case AGENT, DEVICE -> parseJid(source, offset, length, splitOffset, state);
                };
            }
            case ProtobufString.Value value -> Jid.of(value.toString());
            case null -> null;
        };
    }

    private static Jid parseJid(byte[] source, int sourceOffset, int sourceLength) {
        var user = parseUser(source, sourceOffset, sourceLength, sourceLength);
        return new Jid(user, JidServer.whatsapp(), 0, 0);
    }

    private static Jid parseJid(byte[] source, int sourceOffset, int sourceLength, int userLength) {
        var user = parseUser(source, sourceOffset, userLength, userLength);
        var serverOffset = userLength + 1;
        var server = JidServer.of(source, sourceOffset + serverOffset, sourceLength - serverOffset);
        return new Jid(user, server, 0, 0);
    }

    private static Jid parseJid(byte[] source, int sourceOffset, int sourceLength, int userLength, ParserState state) {
        var agent = 0;
        var device = 0;
        var server = JidServer.whatsapp();
        var errorPosition = -1;
        agentAndDeviceLabel: {
            for (var parserPosition = userLength + 1; parserPosition < sourceLength; parserPosition++) {
                var token = getAsciiChar(source, sourceOffset + parserPosition);
                switch (state) {
                    case DEVICE -> {
                        if (token == '@') {
                            var serverOffset = sourceOffset + parserPosition + 1;
                            server = JidServer.of(source, serverOffset, sourceLength - serverOffset);
                            break agentAndDeviceLabel;
                        } else if (token == ':') {
                            device = 0;
                        } else if (token == '_') {
                            agent = 0;
                            state = ParserState.AGENT;
                        } else if(Character.isDigit(token)) {
                            device = device * 10 + (token - '0');
                        }else {
                            errorPosition = parserPosition;
                            break agentAndDeviceLabel;
                        }
                    }
                    case AGENT -> {
                        if (token == '@') {
                            var serverOffset = sourceOffset + parserPosition + 1;
                            server = JidServer.of(source, serverOffset, sourceLength - serverOffset);
                            break agentAndDeviceLabel;
                        } else if (token == ':') {
                            device = 0;
                            state = ParserState.DEVICE;
                        } else if (token == '_') {
                            agent = 0;
                        } else if(Character.isDigit(token)) {
                            agent = agent * 10 + (token - '0');
                        }else {
                            errorPosition = parserPosition;
                            break agentAndDeviceLabel;
                        }
                    }
                }
            }
        }
        if(errorPosition != -1) {
            while (errorPosition < sourceLength) {
                if (getAsciiChar(source, sourceOffset + errorPosition) == '@') {
                    return parseJid(source, sourceOffset, sourceLength, errorPosition);
                }
                errorPosition++;
            }
            return parseJid(source, sourceOffset, sourceLength, errorPosition);
        }else {
            var user = parseUser(source, sourceOffset, userLength, userLength);
            return new Jid(user, server, device, agent);
        }
    }

    private static String parseUser(byte[] source, int sourceOffset, int sourceLength, int outputLength) {
        var output = new char[outputLength];
        for (var position = 0; position < sourceLength; position++) {
            output[position] = getAsciiChar(source, sourceOffset + position);
        }
        return new String(output);
    }

    private static char getAsciiChar(byte[] source, int offset) {
        return (char) (source[offset] & 0x7F);
    }

    private enum ParserState {
        USER,
        DEVICE,
        AGENT,
        SERVER
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @return a non-null contact jid
     */
    @Json.Creator
    public static Jid of(String jid) {
        var serverSeparatorIndex = jid.indexOf("@");
        if (serverSeparatorIndex == -1) {
            return parseJid(jid, JidServer.whatsapp(), serverSeparatorIndex);
        }else {
            var serverOffset = serverSeparatorIndex + 1;
            var server = JidServer.of(jid, serverOffset, jid.length() - serverOffset);
            return parseJid(jid, server, serverSeparatorIndex);
        }
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(String jid, JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return parseJid(jid, server, jid.indexOf("@"));
    }

    private static Jid parseJid(String jid, JidServer server, int userLength) {
        var length = userLength == -1 ? jid.length() : userLength;
        if (length == 0) {
            return new Jid(null, server, 0, 0);
        }

        var offset = jid.charAt(0) == '+' ? 1 : 0;
        if (offset >= length) {
            return new Jid(null, server, 0, 0);
        }

        var deviceIndex = -1;
        var agentIndex = -1;
        var index = offset;
        while (index < length && (deviceIndex == -1 || agentIndex == -1)) {
            switch (jid.charAt(index)) {
                case ':' -> deviceIndex = index;
                case '_' -> agentIndex = index;
            }
            index++;
        }

        if (deviceIndex == -1 && agentIndex == -1) {
            return parseJid(jid, offset, length, server, userLength);
        }else if (deviceIndex != -1 && agentIndex != -1) {
            if(agentIndex < deviceIndex) {
                var user = jid.substring(offset, agentIndex);

                var agent = 0;
                while (agentIndex < deviceIndex - 1) {
                    var digit = jid.charAt(++agentIndex);
                    if (Character.isDigit(digit)) {
                        agent = agent * 10 + (digit - '0');
                    }else {
                        return parseJid(jid, offset, length, server, userLength);
                    }
                }

                var device = 0;
                while (deviceIndex < length - 1) {
                    var digit = jid.charAt(++deviceIndex);
                    if (Character.isDigit(digit)) {
                        device = device * 10 + (digit - '0');
                    }else {
                        return parseJid(jid, offset, length, server, userLength);
                    }
                }

                return new Jid(user, server, device, agent);
            }else {
                var user = jid.substring(offset, deviceIndex);

                var device = 0;
                while (deviceIndex < agentIndex - 1) {
                    var digit = jid.charAt(++deviceIndex);
                    if (Character.isDigit(digit)) {
                        device = device * 10 + (digit - '0');
                    }else {
                        return parseJid(jid, offset, length, server, userLength);
                    }
                }

                var agent = 0;
                while (agentIndex < length - 1) {
                    var digit = jid.charAt(++agentIndex);
                    if (Character.isDigit(digit)) {
                        agent = agent * 10 + (digit - '0');
                    }else {
                        return parseJid(jid, offset, length, server, userLength);
                    }
                }

                return new Jid(user, server, device, agent);
            }
        } else if (deviceIndex != -1) {
            var user = jid.substring(offset, deviceIndex);

            var device = 0;
            while (deviceIndex < length - 1) {
                var digit = jid.charAt(++deviceIndex);
                if (Character.isDigit(digit)) {
                    device = device * 10 + (digit - '0');
                }else {
                    return parseJid(jid, offset, length, server, userLength);
                }
            }

            return new Jid(user, server, device, 0);
        } else {
            var user = jid.substring(offset, agentIndex);

            var agent = 0;
            while (agentIndex < length - 1) {
                var digit = jid.charAt(++agentIndex);
                if (Character.isDigit(digit)) {
                    agent = agent * 10 + (digit - '0');
                }else {
                    return parseJid(jid, offset, length, server, userLength);
                }
            }

            return new Jid(user, server, 0, agent);
        }
    }

    private static Jid parseJid(String source, int sourceOffset, int sourceLength, JidServer server, int userLength) {
        var user = userLength == -1 && sourceOffset == 0 ? source : source.substring(sourceOffset, sourceLength);
        return new Jid(user, server, 0, 0);
    }

    /**
     * Returns the type of this jid
     *
     * @return a non-null type
     */
    // FIXME: Update this method
    public Type type() {
        return isCompanion() ? Type.COMPANION : switch (server().type()) {
            case WHATSAPP -> Objects.equals(user(), "16505361212") ? Type.OFFICIAL_SURVEY_ACCOUNT : Type.USER;
            case LID -> Type.LID;
            case BROADCAST -> Objects.equals(user(), "status") ? Type.STATUS : Type.BROADCAST;
            case GROUP_OR_COMMUNITY -> Type.GROUP;
            case CALL -> Type.CALL;
            case NEWSLETTER -> Type.NEWSLETTER;
            case USER -> switch (user()) {
                case "server" -> Type.SERVER;
                case "0" -> Type.ANNOUNCEMENT;
                case "16508638904" -> Type.IAS;
                case "16505361212" -> Type.OFFICIAL_BUSINESS_ACCOUNT;
                case null, default -> Type.UNKNOWN;
            };
            case UNKNOWN -> Type.UNKNOWN;
        };
    }

    /**
     * Returns whether this jid is associated with a companion device
     *
     * @return true if this jid is a companion
     */
    public boolean isCompanion() {
        return device() != 0;
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
        return new Jid(user(), server, device, agent);
    }

    /**
     * Returns a new jid using with a different agent
     *
     * @param agent the new agent
     * @return a non-null jid
     */
    public Jid withAgent(int agent) {
        return new Jid(user, server, device, agent);
    }
    /**
     * Returns a new jid using with a different device
     *
     * @param device the new device
     * @return a non-null jid
     */
    public Jid withDevice(int device) {
        return new Jid(user, server, device, agent);
    }

    /**
     * Converts this jid to a user jid
     *
     * @return a non-null jid
     */
    public Jid toSimpleJid() {
        return new Jid(user, server, 0, 0);
    }

    /**
     * Converts this jid to a non-formatted phone number
     *
     * @return a non-null String
     */
    public Optional<String> toPhoneNumber() {
        try {
            Long.parseLong(user);
            return Optional.of("+" + user);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Converts this jid to a String
     *
     * @return a non-null String
     */
    @Json.Value
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
        var agent = hasAgent ? "_" + this.agent : "";
        var device = hasDevice ? ":" + this.device : "";
        return user + agent + device + "@" + server.toString();
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

    /**
     * The constants of this enumerated type describe the various types of jids currently supported
     */
    public enum Type {
        /**
         * Represents a device connected using the multi device beta
         */
        COMPANION,
        /**
         * Regular Whatsapp contact Jid
         */
        USER,
        /**
         * Official survey account
         */
        OFFICIAL_SURVEY_ACCOUNT,
        /**
         * Lid
         */
        LID,
        /**
         * Broadcast list
         */
        BROADCAST,
        /**
         * Official business account
         */
        OFFICIAL_BUSINESS_ACCOUNT,
        /**
         * Group Chat Jid
         */
        GROUP,
        /**
         * Group Call Jid
         */
        CALL,
        /**
         * Server Jid: Used to send nodes to Whatsapp usually
         */
        SERVER,
        /**
         * Announcements Chat Jid: Read only chat, usually used by Whatsapp for log updates
         */
        ANNOUNCEMENT,
        /**
         * IAS Chat jid
         */
        IAS,
        /**
         * Image Status Jid of a contact
         */
        STATUS,
        /**
         * Unknown Jid type
         */
        UNKNOWN,
        /**
         * Newsletter
         */
        NEWSLETTER
    }
}