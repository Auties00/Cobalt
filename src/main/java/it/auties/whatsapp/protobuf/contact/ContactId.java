package it.auties.whatsapp.protobuf.contact;

import it.auties.whatsapp.api.Whatsapp;
import lombok.Builder;
import lombok.NonNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a jid.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 * This class also offers a builder, accessible using {@link ContactId#builder()}.
 */
@Builder
public record ContactId(String user, int device, int agent, @NonNull String server, boolean companion) {
    /**
     * The official business account address
     */
    public static final String OFFICIAL_BUSINESS_ACCOUNT = "16505361212@c.us";

    /**
     * The at for all Whatsapp users
     */
    public static final String USER_ADDRESS = "s.whatsapp.net";

    /**
     * The ID of Whatsapp, used to send nodes
     */
    public static final ContactId WHATSAPP = ofServer(USER_ADDRESS);

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact id
     */
    public static ContactId of(@NonNull String jid) {
        return new ContactId(parseId(jid), 0, 0, USER_ADDRESS, false);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact id
     */
    public static ContactId of(String jid, @NonNull String server) {
        return new ContactId(parseId(jid), 0, 0, server, false);
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact id
     */
    public static ContactId ofServer(@NonNull String server) {
        return new ContactId(null, 0, 0, server, false);
    }

    /**
     * Constructs a new ContactId for a companion
     *
     * @param jid    the nullable jid of the user
     * @param agent  the agent id
     * @param device the device id
     * @return a non-null contact id
     */
    public static ContactId ofCompanion(String jid, int agent, int device) {
        return new ContactId(parseId(jid), device, agent, USER_ADDRESS, true);
    }

    /**
     * Constructs a new ContactId from an encoded value
     *
     * @param encoded the nullable encoded value
     * @return a non-null optional ContactId
     */
    public static Optional<ContactId> ofEncoded(String encoded) {
        if(encoded == null || !encoded.contains("@")){
            return Optional.empty();
        }

        try {
            var atIndex = encoded.indexOf("@");
            var server = encoded.substring(atIndex + 1);
            var userAndDevice = encoded.substring(0, atIndex).split(":", 2);
            var userAndAgent = userAndDevice[0].split("_");
            var user = userAndAgent[0];
            var agent = Integer.parseInt(userAndAgent[1]);
            var device = Integer.parseInt(userAndDevice[1]);
            return Optional.of(new ContactId(user, device, agent, server, false));
        }catch (Exception exception){
            return Optional.empty();
        }
    }

    /**
     * Parses a nullable jid to the Whatsapp Jid Format
     *
     * @param jid the nullable jid to parse
     * @return null if {@code jid == null}, otherwise a non null string
     */
    public static String parseId(String jid) {
        if(jid == null) {
            return null;
        }

        return jid.replace("@c.us", "")
                .replace("@s.whatsapp.net", "")
                .replace("@g.us", "");
    }

    /**
     * Returns the type of this jid
     *
     * @return a non null type
     */
    public Type type() {
        if (device() != 0) {
            return Type.COMPANION;
        }

        if (Objects.equals(server(), "s.whatsapp.net")) {
            return Type.USER;
        }

        if (Objects.equals(server(), "broadcast")) {
            return Type.BROADCAST;
        }

        if (toString().equals(OFFICIAL_BUSINESS_ACCOUNT)) {
            return Type.OFFICIAL_BUSINESS_ACCOUNT;
        }

        if (Objects.equals(server(), "g.us")) {
            return Type.SERVER;
        }

        if (Objects.equals(server(), "call")) {
            return Type.GROUP_CALL;
        }

        if (Objects.equals(server(), "c.us") && Objects.equals(user(), "server")) {
            return Type.SERVER;
        }

        if (Objects.equals(server(), "c.us") && Objects.equals(user(), "0")) {
            return Type.ANNOUNCEMENT;
        }

        if(Objects.equals(user(), "status") && Objects.equals(server(), "broadcast")){
            return Type.STATUS;
        }

        return Type.UNKNOWN;
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
         * Image Status Jid of a contact
         */
        STATUS,

        /**
         * Unknown Jid type
         */
        UNKNOWN
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContactId jid
                && Objects.equals(server(), jid.server());
    }

    @Override
    public String toString() {
        if (!companion()) {
            var user = Objects.requireNonNullElse(user(), "");
            var agent = agent() != 0 ? "_%s".formatted(agent()) : "";
            var device = device() != 0 ? ":%s".formatted(device()) : "";
            return "%s%s%s@%s".formatted(user, agent, device, server());
        }

        if (agent == 0 && device == 0) {
            return "%s@%s".formatted(user, USER_ADDRESS);
        }

        if (agent != 0 && device == 0) {
            return "%s.%s@%s".formatted(user, agent, USER_ADDRESS);
        }

        if (agent == 0) {
            return "%s:%s@%s".formatted(user, device, USER_ADDRESS);
        }

        return "%s.%s:%s@%s".formatted(user, agent, device, USER_ADDRESS);
    }
}