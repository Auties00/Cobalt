package it.auties.whatsapp.model.contact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.base.ProtobufConverter;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Objects;

/**
 * A model class that represents a jid. This class is only a model, this means that changing its
 * values will have no real effect on WhatsappWeb's servers. This class also offers a builder,
 * accessible using {@link ContactJid#builder()}.
 */
@Builder
@With
public record ContactJid(String user, @NonNull Server server, int device, int agent) implements ProtobufMessage, ContactJidProvider {
    /**
     * Default constructor
     */
    public ContactJid(String user, @NonNull Server server, int device, int agent){
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
    public static ContactJid ofServer(@NonNull Server server) {
        return of(null, server);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact jid
     */
    public static ContactJid of(String jid, @NonNull Server server) {
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
        for (var server : Server.values()) {
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
        return new ContactJid(withoutServer(jid), Server.WHATSAPP, device, agent);
    }

    /**
     * Constructs a new ContactId for a device
     *
     * @param jid    the nullable jid of the user
     * @param device the device jid
     * @return a non-null contact jid
     */
    public static ContactJid ofDevice(String jid, int device) {
        return new ContactJid(withoutServer(jid), Server.WHATSAPP, device, 0);
    }

    /**
     * Do not use this method, reserved for protobuf
     */
    @ProtobufConverter
    public static ContactJid ofProtobuf(Object input) {
        return input == null ? null : of((String) input);
    }

    @ProtobufConverter
    public Object toValue() {
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
        return of(jid, Server.of(jid));
    }

    /**
     * Returns the type of this jid
     *
     * @return a non null type
     */
    public Type type() {
        return isCompanion() ? Type.COMPANION : switch (server()) {
            case WHATSAPP -> Objects.equals(user(), "16505361212") ? Type.OFFICIAL_SURVEY_ACCOUNT : Type.USER;
            case LID -> Type.LID;
            case BROADCAST -> Objects.equals(user(), "status") ? Type.STATUS : Type.BROADCAST;
            case GROUP -> Type.GROUP;
            case GROUP_CALL -> Type.GROUP_CALL;
            case USER -> switch (user()) {
                case "server" -> Type.SERVER;
                case "0" -> Type.ANNOUNCEMENT;
                case "16508638904" -> Type.IAS;
                case "16505361212" -> Type.OFFICIAL_BUSINESS_ACCOUNT;
                default -> Type.UNKNOWN;
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
    public boolean hasServer(Server server) {
        return server() == server;
    }

    /**
     * Returns whether this jid is a server jid
     *
     * @param server the server to check against
     * @return a boolean
     */
    public boolean isServerJid(Server server) {
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
        var name = toString().split("@", 2)[0];
        return new SessionAddress(name, 0);
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
        GROUP_CALL,
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
        UNKNOWN
    }

    /**
     * The constants of this enumerated type describe the various servers that a jid might be linked
     * to
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Server {
        /**
         * User
         */
        USER("c.us"),
        /**
         * Group
         */
        GROUP("g.us"),
        /**
         * Broadcast group
         */
        BROADCAST("broadcast"),
        /**
         * Group call
         */
        GROUP_CALL("call"),
        /**
         * Whatsapp
         */
        WHATSAPP("s.whatsapp.net"),
        /**
         * Lid
         */
        LID("lid");

        @Getter
        private final String address;

        @JsonCreator
        public static Server of(String address) {
            return Arrays.stream(values())
                    .filter(entry -> address != null && address.endsWith(entry.address()))
                    .findFirst()
                    .orElse(WHATSAPP);
        }

        public ContactJid toJid() {
            return ofServer(this);
        }

        @Override
        @JsonValue
        public String toString() {
            return address();
        }
    }
}