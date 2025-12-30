package com.github.auties00.cobalt.model.jid;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.libsignal.SignalProtocolAddress;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufString;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.auties00.cobalt.model.jid.JidConstants.*;

/**
 * A record that represents a WhatsApp JID
 */
public record Jid(String user, JidServer server, int device, int agent) implements JidProvider {
    private static final ConcurrentMap<JidServer, Jid> JID_SERVER_CACHE = new ConcurrentHashMap<>();
    
    private static final Jid LEGACY_USER_SERVER = new Jid(null, JidServer.legacyUser());
    private static final Jid GROUP_OR_COMMUNITY_SERVER = new Jid(null, JidServer.groupOrCommunity());
    private static final Jid BROADCAST_SERVER = new Jid(null, JidServer.broadcast());
    private static final Jid CALL_SERVER = new Jid(null, JidServer.call());
    private static final Jid USER_SERVER = new Jid(null, JidServer.user());
    private static final Jid LID_SERVER = new Jid(null, JidServer.lid());
    private static final Jid NEWSLETTER_SERVER = new Jid(null, JidServer.newsletter());
    private static final Jid BOT_SERVER = new Jid(null, JidServer.bot());
    private static final Jid HOSTED_SERVER = new Jid(null, JidServer.hosted());
    private static final Jid HOSTED_LID_SERVER = new Jid(null, JidServer.hostedLid());
    private static final Jid MSGR_SERVER = new Jid(null, JidServer.messenger());
    private static final Jid INTEROP_SERVER = new Jid(null, JidServer.interop());
    
    private static final Jid OFFICIAL_SURVEYS_ACCOUNT = new Jid("16505361212", JidServer.user());
    private static final Jid OFFICIAL_BUSINESS_ACCOUNT = new Jid("16505361212", JidServer.legacyUser());
    private static final Jid ANNOUNCEMENTS_ACCOUNT = new Jid("0", JidServer.user());

    private static final Jid LOCATION_BROADCAST = new Jid("location", JidServer.broadcast());

    public Jid {
        Objects.requireNonNull(server, "server cannot be null");
        checkUnsignedByte(device);
        checkUnsignedByte(agent);
    }
    
    public Jid(String user, JidServer server) {
        this(user, server, 0, 0);
    }

    public static Jid legacyUserServer() {
        return Jid.LEGACY_USER_SERVER;
    }

    public static Jid groupOrCommunityServer() {
        return Jid.GROUP_OR_COMMUNITY_SERVER;
    }

    public static Jid broadcastServer() {
        return Jid.BROADCAST_SERVER;
    }

    public static Jid callServer() {
        return Jid.CALL_SERVER;
    }

    public static Jid userServer() {
        return Jid.USER_SERVER;
    }

    public static Jid lidServer() {
        return Jid.LID_SERVER;
    }

    public static Jid newsletterServer() {
        return Jid.NEWSLETTER_SERVER;
    }

    public static Jid botServer() {
        return Jid.BOT_SERVER;
    }

    public static Jid hostedServer() {
        return Jid.HOSTED_SERVER;
    }

    public static Jid hostedLidServer() {
        return Jid.HOSTED_LID_SERVER;
    }

    public static Jid msgrServer() {
        return Jid.MSGR_SERVER;
    }

    public static Jid interopServer() {
        return Jid.INTEROP_SERVER;
    }

    public static Jid officialSurveysAccount() {
        return Jid.OFFICIAL_SURVEYS_ACCOUNT;
    }

    public static Jid officialBusinessAccount() {
        return Jid.OFFICIAL_BUSINESS_ACCOUNT;
    }

    public static Jid statusBroadcastAccount() {
        return Jid.BROADCAST_SERVER;
    }

    public static Jid announcementsAccount() {
        return Jid.ANNOUNCEMENTS_ACCOUNT;
    }

    public static Jid locationBroadcast() {
        return Jid.LOCATION_BROADCAST;
    }

    public static Jid of(JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return switch (server.type()) {
            case UNKNOWN -> JID_SERVER_CACHE.computeIfAbsent(server, _ -> new Jid(null, server));
            case LEGACY_USER -> legacyUserServer();
            case GROUP_OR_COMMUNITY -> groupOrCommunityServer();
            case BROADCAST -> broadcastServer();
            case CALL -> callServer();
            case USER -> userServer();
            case LID -> lidServer();
            case NEWSLETTER -> newsletterServer();
            case BOT -> botServer();
            case HOSTED -> hostedServer();
            case HOSTED_LID -> hostedLidServer();
            case MSGR -> msgrServer();
            case INTEROP -> interopServer();
        };
    }

    public static Jid of(String user, JidServer server, int device, int agent) {
        if (user == null) {
            return of(server);
        } else {
            return new Jid(user, server, device, agent);
        }
    }

    public static Jid of(long jid) {
        if (jid < 0) {
            throw new MalformedJidException("value cannot be negative");
        }
        return new Jid(String.valueOf(jid), JidServer.user());
    }

    public static Jid of(String jid) {
        if (jid == null) {
            return null;
        }
        var knownServer = JidServer.of(jid, false);
        if (knownServer != null) {
            return of(knownServer);
        }
        var serverSeparatorIndex = jid.indexOf(SERVER_CHAR);
        var server = serverSeparatorIndex == -1
                ? JidServer.user()
                : JidServer.of(jid, serverSeparatorIndex + 1, jid.length() - serverSeparatorIndex - 1);
        return parseJid(jid, serverSeparatorIndex, server);
    }

    public static Jid of(String user, JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return parseJid(user, user.indexOf(SERVER_CHAR), server);
    }

    @ProtobufDeserializer
    public static Jid of(ProtobufString jid) {
        return switch (jid) {
            case ProtobufString.Lazy lazy -> Jid.of(lazy);
            case ProtobufString.Value value -> Jid.of(value.toString());
            case null -> null;
        };
    }

    public static Jid of(ProtobufString.Lazy jid) {
        if (jid == null) {
            return null;
        }
        var source = jid.encodedBytes();
        var offset = jid.encodedOffset();
        var length = jid.encodedLength();
        var knownServer = JidServer.of(source, offset, length, false);
        if (knownServer != null) {
            return of(knownServer);
        }

        enum ParserState { USER, DEVICE, AGENT }

        var state = ParserState.USER;
        var userLength = length;
        var agent = 0;
        var device = 0;
        var server = JidServer.user();
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = (char) (source[offset + parserPosition] & 0x7F);
            if (token == SERVER_CHAR) {
                if (state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(source, offset + parserPosition + 1, length - parserPosition - 1, true);
                break;
            }
            switch (state) {
                case USER -> {
                    if (token == DEVICE_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    } else if (token == AGENT_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if (agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if (Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    } else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + value + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if (device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if (Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    } else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + value + "'");
                    }
                }
            }
        }
        var user = new String(source, offset, userLength, StandardCharsets.UTF_8);
        return new Jid(user, server, device, agent);
    }

    private static void checkUnsignedByte(int i) {
        if (i < 0 || i > 255) {
            throw new MalformedJidException(i + " is not an unsigned byte");
        }
    }

    private static Jid parseJid(String jid, int jidLength, JidServer server) {
        var length = jidLength == -1 ? jid.length() : jidLength;
        if (length == 0) {
            return of(server);
        }
        var offset = jid.charAt(0) == PHONE_CHAR ? 1 : 0;
        if (offset >= length) {
            throw new MalformedJidException("Malformed value '" + jid + "'");
        }

        enum ParserState { USER, DEVICE, AGENT }

        var state = ParserState.USER;
        var userLength = length;
        var agent = 0;
        var device = 0;
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = jid.charAt(offset + parserPosition);
            if (token == SERVER_CHAR) {
                if (state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(jid, offset + parserPosition + 1, length - parserPosition - 1);
                break;
            }
            switch (state) {
                case USER -> {
                    if (token == DEVICE_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    } else if (token == AGENT_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if (agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if (Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    } else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if (device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if (Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    } else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                    }
                }
            }
        }
        var user = jid.substring(offset, offset + userLength);
        return new Jid(user, server, device, agent);
    }
    
    @Override
    public String toString() {
        var hasUser = this.hasUser();
        var hasAgent = this.hasAgent();
        var hasDevice = this.hasDevice();
        if (!hasUser && !hasAgent && !hasDevice) {
            return this.server().toString();
        }
        var user = hasUser ? this.user() : "";
        var agentStr = hasAgent ? "" + AGENT_CHAR + this.agent() : "";
        var deviceStr = hasDevice ? "" + DEVICE_CHAR + this.device() : "";
        return user + agentStr + deviceStr + SERVER_CHAR + this.server().toString();
    }

    @ProtobufSerializer
    public String toProtobufString() {
        return toString();
    }
    
    public boolean hasUser() {
        return user != null;
    }

    public boolean hasUser(String user) {
        return Objects.equals(this.user, user);
    }
    
    public boolean hasServer(JidServer server) {
        return this.server().equals(server);
    }

    public boolean hasDevice() {
        return device != 0;
    }
    
    public boolean hasDevice(int device) {
        return this.device == device;
    }

    public boolean hasAgent() {
        return agent != 0;
    }
    
    public boolean hasAgent(int agent) {
        return this.agent == agent;
    }

    public boolean isServerJid(JidServer server) {
        return user() == null && this.server().equals(server);
    }

    public boolean hasLidServer() {
        return hasServer(JidServer.lid());
    }

    public boolean hasUserServer() {
        return hasServer(JidServer.user()) || hasServer(JidServer.legacyUser());
    }

    public boolean hasGroupOrCommunityServer() {
        return hasServer(JidServer.groupOrCommunity());
    }

    public boolean hasBroadcastServer() {
        return hasServer(JidServer.broadcast());
    }

    public boolean isStatusBroadcastAccount() {
        return statusBroadcastAccount().equals(this);
    }

    public boolean hasNewsletterServer() {
        return hasServer(JidServer.newsletter());
    }

    public boolean hasBotServer() {
        return hasServer(JidServer.bot());
    }

    public boolean hasCallServer() {
        return hasServer(JidServer.call());
    }

    public boolean hasMessengerServer() {
        return hasServer(JidServer.messenger());
    }

    public boolean hasInteropServer() {
        return hasServer(JidServer.interop());
    }

    public boolean hasHostedServer() {
        return hasServer(JidServer.hosted());
    }

    public boolean hasHostedLidServer() {
        return hasServer(JidServer.hostedLid());
    }

    public Jid withServer(JidServer server) {
        if (Objects.equals(this.server, server)) {
            return this;
        }
        return new Jid(user, server, device, agent);
    }

    public Jid withAgent(int agent) {
        if (this.agent == agent) {
            return this;
        }
        return new Jid(user, server, device, agent);
    }

    public Jid withDevice(int device) {
        if (this.device == device) {
            return this;
        }
        return new Jid(user, server, device, agent);
    }

    public Jid withoutData() {
        if (!hasDevice() && !hasAgent()) {
            return this;
        }
        return new Jid(user, server);
    }

    public Jid toUserJid() {
        var server = server();
        if (server.equals(JidServer.hosted())) {
            return Jid.of(user(), JidServer.user());
        }
        if (server.equals(JidServer.hostedLid())) {
            return Jid.of(user(), JidServer.lid());
        }
        return withoutData();
    }

    public Optional<String> toPhoneNumber() {
        var user = user();
        if (user == null) {
            return Optional.empty();
        }
        for (var i = 0; i < user.length(); i++) {
            if (!Character.isDigit(user.charAt(i))) {
                return Optional.empty();
            }
        }
        return Optional.of('+' + user);
    }

    public SignalProtocolAddress toSignalAddress() {
        return new SignalProtocolAddress(user(), device());
    }

    @Override
    public Jid toJid() {
        return this;
    }
}
