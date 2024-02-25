package it.auties.whatsapp.model.jid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.whatsapp.model.signal.session.SessionAddress;

import java.util.Objects;

/**
 * A model class that represents a jid. This class is only a model, this means that changing its
 * values will have no real effect on WhatsappWeb's servers.
 */
public record Jid(String user, JidServer server, Integer device, Integer agent) implements JidProvider {
    /**
     * Default constructor
     */
    public Jid(String user, JidServer server, Integer device, Integer agent) {
        this.user = user != null && user.startsWith("+") ? user.substring(1) : user;
        this.server = server;
        this.device = device;
        this.agent = agent;
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid ofServer(JidServer server) {
        return of(null, server);
    }

    @ProtobufConverter // Reserved for protobuf
    public static Jid ofProtobuf(String input) {
        return input == null ? null : Jid.of(input);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static Jid of(String jid, JidServer server) {
        var complexUser = withoutServer(jid);
        if (complexUser == null) {
            return new Jid(null, server, null, null);
        }

        if (complexUser.contains(":")) {
            var simpleUser = complexUser.split(":", 2);
            var user = simpleUser[0];
            var device = Integer.parseUnsignedInt(simpleUser[1]);
            if (user.contains("_")) {
                var simpleUserAgent = user.split("_", 2);
                var agent = tryParseAgent(simpleUserAgent[1]);
                return new Jid(simpleUserAgent[0], server, device, agent);
            }
            return new Jid(user, server, device, null);
        }

        if (!complexUser.contains("_")) {
            return new Jid(complexUser, server, null, null);
        }

        var simpleUserAgent = complexUser.split("_", 2);
        var agent = tryParseAgent(simpleUserAgent[1]);
        return new Jid(simpleUserAgent[0], server, null, agent);
    }

    /**
     * Parses a nullable jid to the Whatsapp Jid Format
     *
     * @param jid the nullable jid to parse
     * @return null if {@code jid == null}, otherwise a non-null string
     */
    public static String withoutServer(String jid) {
        if (jid == null) {
            return null;
        }
        for (var server : JidServer.values()) {
            jid = jid.replace("@%s".formatted(server), "");
        }
        return jid;
    }

    private static Integer tryParseAgent(String string) {
        try {
            return Integer.parseUnsignedInt(string);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Constructs a new ContactId for a device
     *
     * @param jid    the nullable jid of the user
     * @param device the device jid
     * @return a non-null contact jid
     */
    public static Jid ofDevice(String jid, int device) {
        return new Jid(withoutServer(jid), JidServer.WHATSAPP, device, null);
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    @JsonCreator
    public static Jid of(String jid) {
        return of(jid, JidServer.of(jid));
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    public static Jid of(long jid) {
        return of(String.valueOf(jid), JidServer.WHATSAPP);
    }

    /**
     * Returns the type of this jid
     *
     * @return a non null type
     */
    public JidType type() {
        return isCompanion() ? JidType.COMPANION : switch (server()) {
            case WHATSAPP -> Objects.equals(user(), "16505361212") ? JidType.OFFICIAL_SURVEY_ACCOUNT : JidType.USER;
            case LID -> JidType.LID;
            case BROADCAST -> Objects.equals(user(), "status") ? JidType.STATUS : JidType.BROADCAST;
            case GROUP -> JidType.GROUP;
            case GROUP_CALL -> JidType.GROUP_CALL;
            case NEWSLETTER -> JidType.NEWSLETTER;
            case USER -> switch (user()) {
                case "server" -> JidType.SERVER;
                case "0" -> JidType.ANNOUNCEMENT;
                case "16508638904" -> JidType.IAS;
                case "16505361212" -> JidType.OFFICIAL_BUSINESS_ACCOUNT;
                default -> JidType.UNKNOWN;
            };
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
        return server() == server;
    }

    /**
     * Returns whether this jid is a server jid
     *
     * @param server the server to check against
     * @return a boolean
     */
    public boolean isServerJid(JidServer server) {
        return user() == null && server() == server;
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
     * Converts this jid to a user jid
     *
     * @return a non-null jid
     */
    public Jid toSimpleJid() {
        return of(user(), server());
    }

    /**
     * Converts this jid to a non-formatted phone number
     *
     * @return a non-null String
     */
    public String toPhoneNumber() {
        return "+%s".formatted(user);
    }

    /**
     * Converts this jid to a String
     *
     * @return a non-null String
     */
    @JsonValue
    @ProtobufConverter
    @Override
    public String toString() {
        var user = Objects.requireNonNullElse(user(), "");
        var agent = hasAgent() ? "_%s".formatted(agent()) : "";
        var device = hasDevice() ? ":%s".formatted(device()) : "";
        var leading = "%s%s%s".formatted(user, agent, device);
        return leading.isEmpty() ? server().toString() : "%s@%s".formatted(leading, server());
    }

    /**
     * Converts this jid to a signal address
     *
     * @return a non-null {@link SessionAddress}
     */
    public SessionAddress toSignalAddress() {
        return new SessionAddress(user(), device());
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

    @Override
    public Integer device() {
        return Objects.requireNonNullElse(device, 0);
    }

    /**
     * Returns whether this jid specifies a device
     *
     * @return a boolean
     */
    public boolean hasDevice() {
        return device != null;
    }

    @Override
    public Integer agent() {
        return Objects.requireNonNullElse(agent, 0);
    }

    /**
     * Returns whether this jid specifies an agent
     *
     * @return a boolean
     */
    public boolean hasAgent() {
        return agent != null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toString());
    }
}