package it.auties.whatsapp.model.jid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.signal.session.SessionAddress;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A model class that represents a jid. This class is only a model, this means that changing its
 * values will have no real effect on WhatsappWeb's servers.
 */
public final class Jid implements JidProvider {
    private final String user;
    private final JidServer server;
    private final int device;
    private final int agent;

    private Jid(String user, JidServer server, int device, int agent) {
        this.user = user;
        this.server = server;
        this.device = device;
        this.agent = agent;
    }

    public static Jid of(String user, JidServer server, int device, int agent) {
        return new Jid(hasPhoneNumberPrefix(user) ? user.substring(1) : user, server, device, agent);
    }

    private static boolean hasPhoneNumberPrefix(String user) {
        return user != null && !user.isEmpty() && user.charAt(0) == '+';
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    @ProtobufDeserializer
    @JsonCreator
    public static Jid of(String jid) {
        if (jid == null) {
            return null;
        }

        var separator = jid.indexOf("@");
        if (separator == -1) {
            return parseJid(jid, jid.length(), JidServer.whatsapp());
        }

        return parseJid(jid, separator, JidServer.of(jid.substring(separator + 1)));
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(JidServer server) {
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
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(String jid, JidServer server) {
        var separator = jid.indexOf("@");
        return parseJid(jid, separator == -1 ? jid.length() : separator, server);
    }

    private static Jid parseJid(String complexUser, int length, JidServer server) {
        var offset = hasPhoneNumberPrefix(complexUser) ? 1 : 0;

        var deviceSeparator = complexUser.indexOf(":", 0, length);
        var agentSeparator = complexUser.indexOf("_", 0, length);
        var hasDevice = deviceSeparator != -1;
        var hasAgent = agentSeparator != -1;

        if (hasDevice && hasAgent) {
            var user = complexUser.substring(offset, Math.min(deviceSeparator, agentSeparator));
            var maybeDevice = tryParseInt(complexUser.substring(deviceSeparator + 1, agentSeparator > deviceSeparator ? agentSeparator : length));
            if(maybeDevice.isEmpty()) {
                return new Jid(getOrCopyUser(complexUser, length, offset), server, 0, 0);
            }

            var maybeAgent = tryParseInt(complexUser.substring(agentSeparator + 1, deviceSeparator > agentSeparator ? deviceSeparator : length));
            if(maybeAgent.isEmpty()) {
                return new Jid(getOrCopyUser(complexUser, length, offset), server, 0, 0);
            }

            return new Jid(user, server, maybeDevice.getAsInt(), maybeAgent.getAsInt());
        }

        if (hasDevice) {
            var user = complexUser.substring(offset, deviceSeparator);
            var maybeDevice = tryParseInt(complexUser.substring(deviceSeparator + 1, length));
            if(maybeDevice.isEmpty()) {
                return new Jid(getOrCopyUser(complexUser, length, offset), server, 0, 0);
            }

            return new Jid(user, server, maybeDevice.getAsInt(), 0);
        }

        if (hasAgent) {
            var user = complexUser.substring(offset, agentSeparator);
            var maybeAgent = tryParseInt(complexUser.substring(agentSeparator + 1, length));
            if(maybeAgent.isEmpty()) {
                return new Jid(getOrCopyUser(complexUser, length, offset), server, 0, 0);
            }

            return new Jid(user, server, 0, maybeAgent.getAsInt());
        }

        return new Jid(getOrCopyUser(complexUser, length, offset), server, 0, 0);
    }

    private static String getOrCopyUser(String complexUser, int length, int offset) {
        if (offset == 0 && length == complexUser.length()) {
            return complexUser;
        }

        return complexUser.substring(offset, length);
    }

    private static OptionalInt tryParseInt(String string) {
        try {
            return OptionalInt.of(Integer.parseUnsignedInt(string));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the type of this jid
     *
     * @return a non-null type
     */
    public JidType type() {
        return isCompanion() ? JidType.COMPANION : switch (server()) {
            case JidServer.Whatsapp ignored -> Objects.equals(user(), "16505361212") ? JidType.OFFICIAL_SURVEY_ACCOUNT : JidType.USER;
            case JidServer.Lid ignored -> JidType.LID;
            case JidServer.Broadcast ignored -> Objects.equals(user(), "status") ? JidType.STATUS : JidType.BROADCAST;
            case JidServer.GroupOrCommunity ignored -> JidType.GROUP;
            case JidServer.GroupCall ignored -> JidType.GROUP_CALL;
            case JidServer.Newsletter ignored -> JidType.NEWSLETTER;
            case JidServer.User ignored -> switch (user()) {
                case "server" -> JidType.SERVER;
                case "0" -> JidType.ANNOUNCEMENT;
                case "16508638904" -> JidType.IAS;
                case "16505361212" -> JidType.OFFICIAL_BUSINESS_ACCOUNT;
                case null, default -> JidType.UNKNOWN;
            };
            case JidServer.Unknown ignored -> JidType.UNKNOWN;
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

    public Jid withoutAgent() {
        return new Jid(user, server, device, 0);
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

    public Jid withoutDevice() {
        return new Jid(user, server, 0, agent);
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
        }catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Converts this jid to a String
     *
     * @return a non-null String
     */
    @JsonValue
    @ProtobufSerializer
    @Override
    public String toString() {
        var hasUser = hasUser();
        var hasAgent = hasAgent();
        var hasDevice = hasDevice();
        if(!hasUser && !hasAgent && !hasDevice) {
            return server.toString();
        }

        var user = hasUser ? this.user : "";
        var agent = hasAgent ? "_" + this.agent: "";
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

    public String user() {
        return user;
    }

    public JidServer server() {
        return server;
    }

    public int device() {
        return device;
    }

    public int agent() {
        return agent;
    }

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
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Jid) obj;
        return Objects.equals(this.user, that.user) &&
                Objects.equals(this.server, that.server) &&
                this.device == that.device &&
                this.agent == that.agent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, server, device, agent);
    }
}