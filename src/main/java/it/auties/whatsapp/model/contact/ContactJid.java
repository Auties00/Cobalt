package it.auties.whatsapp.model.contact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * A model class that represents a jid. This class is only a model, this means that changing its
 * values will have no real effect on WhatsappWeb's servers.
 */
public record ContactJid(String user, @NonNull ContactJidServer server, int device, int agent) implements ContactJidProvider {
    /**
     * Default constructor
     */
    public ContactJid(String user, @NonNull ContactJidServer server, int device, int agent){
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
    public static ContactJid ofServer(@NonNull ContactJidServer server) {
        return of(null, server);
    }

    @ProtobufConverter // Reserved for protobuf
    public static ContactJid ofProtobuf(@Nullable String input) {
        return input == null ? null : ContactJid.of(input);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static ContactJid of(String jid, @NonNull ContactJidServer server) {
        var complexUser = withoutServer(jid);
        if (complexUser == null) {
            return new ContactJid(null, server, 0, 0);
        }
        if (complexUser.contains(":")) {
            var simpleUser = complexUser.split(":", 2);
            var user = simpleUser[0];
            var device = Integer.parseUnsignedInt(simpleUser[1]);
            if (user.contains("_")) {
                var simpleUserAgent = user.split("_", 2);
                var agent = tryParseAgent(simpleUserAgent[1]);
                return new ContactJid(simpleUserAgent[0], server, device, agent);
            }
            return new ContactJid(user, server, device, 0);
        }
        if (!complexUser.contains("_")) {
            return new ContactJid(complexUser, server, 0, 0);
        }
        var simpleUserAgent = complexUser.split("_", 2);
        var agent = tryParseAgent(simpleUserAgent[1]);
        return new ContactJid(simpleUserAgent[0], server, 0, agent);
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
        for (var server : ContactJidServer.values()) {
            jid = jid.replace("@%s".formatted(server), "");
        }
        return jid;
    }

    private static int tryParseAgent(String string) {
        try {
            return Integer.parseUnsignedInt(string);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    /**
     * Constructs a new ContactId for a device
     *
     * @param jid    the nullable jid of the user
     * @param agent  the agent jid
     * @param device the device jid
     * @return a non-null contact jid
     */
    public static ContactJid ofDevice(String jid, int device, int agent) {
        return new ContactJid(withoutServer(jid), ContactJidServer.WHATSAPP, device, agent);
    }

    /**
     * Constructs a new ContactId for a device
     *
     * @param jid    the nullable jid of the user
     * @param device the device jid
     * @return a non-null contact jid
     */
    public static ContactJid ofDevice(String jid, int device) {
        return new ContactJid(withoutServer(jid), ContactJidServer.WHATSAPP, device, 0);
    }

    @ProtobufConverter
    public String toValue() {
        return toString();
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    @JsonCreator
    public static ContactJid of(@NonNull String jid) {
        return of(jid, ContactJidServer.of(jid));
    }

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact jid
     */
    public static ContactJid of(long jid) {
        return of(String.valueOf(jid), ContactJidServer.WHATSAPP);
    }

    /**
     * Returns the type of this jid
     *
     * @return a non null type
     */
    public ContactJidType type() {
        return isCompanion() ? ContactJidType.COMPANION : switch (server()) {
            case WHATSAPP -> Objects.equals(user(), "16505361212") ? ContactJidType.OFFICIAL_SURVEY_ACCOUNT : ContactJidType.USER;
            case LID -> ContactJidType.LID;
            case BROADCAST -> Objects.equals(user(), "status") ? ContactJidType.STATUS : ContactJidType.BROADCAST;
            case GROUP -> ContactJidType.GROUP;
            case GROUP_CALL -> ContactJidType.GROUP_CALL;
            case USER -> switch (user()) {
                case "server" -> ContactJidType.SERVER;
                case "0" -> ContactJidType.ANNOUNCEMENT;
                case "16508638904" -> ContactJidType.IAS;
                case "16505361212" -> ContactJidType.OFFICIAL_BUSINESS_ACCOUNT;
                default -> ContactJidType.UNKNOWN;
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
    public boolean hasServer(ContactJidServer server) {
        return server() == server;
    }

    /**
     * Returns whether this jid is a server jid
     *
     * @param server the server to check against
     * @return a boolean
     */
    public boolean isServerJid(ContactJidServer server) {
        return user() == null && server() == server;
    }

    /**
     * Converts this jid to a user jid
     *
     * @return a non-null jid
     */
    public ContactJid toWhatsappJid() {
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
    @Override
    public String toString() {
        var user = Objects.requireNonNullElse(user(), "");
        var agent = agent() != 0 ? "_%s".formatted(agent()) : "";
        var device = device() != 0 ? ":%s".formatted(device()) : "";
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
    @NonNull
    public ContactJid toJid() {
        return this;
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
}