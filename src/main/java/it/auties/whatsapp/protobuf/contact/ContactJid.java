package it.auties.whatsapp.protobuf.contact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A model class that represents a jid.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 * This class also offers a builder, accessible using {@link ContactJid#builder()}.
 */
@Builder
@Log
public record ContactJid(String user, @NonNull Server server, int device, int agent) {
    /**
     * The official business account address
     */
    public static final String OFFICIAL_BUSINESS_ACCOUNT = "16505361212@s.whatsapp.net";

    /**
     * The ID of Whatsapp, used to send nodes
     */
    public static final ContactJid SOCKET = ofServer(Server.WHATSAPP);

    /**
     * Constructs a new ContactId for a user from a jid
     *
     * @param jid the non-null jid of the user
     * @return a non-null contact id
     */
    @JsonCreator
    public static ContactJid ofUser(@NonNull String jid) {
        return new ContactJid(withoutServer(jid), Server.WHATSAPP, 0, 0);
    }

    /**
     * Constructs a new ContactId for a user from a jid and a custom server
     *
     * @param jid    the nullable jid of the user
     * @param server the non-null custom server
     * @return a non-null contact id
     */
    public static ContactJid ofUser(String jid, @NonNull Server server) {
        return new ContactJid(withoutServer(jid), server, 0, 0);
    }

    /**
     * Constructs a new ContactId that represents a server
     *
     * @param server the non-null custom server
     * @return a non-null contact id
     */
    public static ContactJid ofServer(@NonNull Server server) {
        return new ContactJid(null, server, 0, 0);
    }

    /**
     * Constructs a new ContactId for a companion
     *
     * @param jid    the nullable jid of the user
     * @param agent  the agent id
     * @param device the device id
     * @return a non-null contact id
     */
    public static ContactJid ofCompanion(String jid, int device, int agent) {
        return new ContactJid(withoutServer(jid), Server.WHATSAPP, device, agent);
    }

    /**
     * Constructs a new ContactId from an encoded value
     *
     * @param encoded the nullable encoded value
     * @return a non-null optional ContactId
     */
    public static ContactJid ofEncoded(String encoded) {
        if(encoded == null || !encoded.contains("@")){
            throw new IllegalArgumentException("Cannot decode %s".formatted(encoded));
        }

        try {
            var atIndex = encoded.indexOf("@");
            var server = encoded.substring(atIndex + 1);
            var userAndDevice = encoded.substring(0, atIndex).split(":", 2);
            var userAndAgent = userAndDevice[0].split("_");
            var user = userAndAgent[0];
            var agent = Integer.parseInt(userAndAgent[1]);
            var device = Integer.parseInt(userAndDevice[1]);
            return new ContactJid(user, Server.forAddress(server), device, agent);
        }catch (Exception exception){
            throw new RuntimeException("Cannot decode %s: %s".formatted(encoded, exception.getMessage()));
        }
    }

    /**
     * Parses a nullable jid to the Whatsapp Jid Format
     *
     * @param jid the nullable jid to parse
     * @return null if {@code jid == null}, otherwise a non null string
     */
    public static String withoutServer(String jid) {
        if(jid == null) {
            return null;
        }

        for(var server : Server.values()) {
            jid = jid.replaceAll(server.toString(), "");
        }

        return jid;
    }

    /**
     * Returns the type of this jid
     *
     * @return a non null type
     */
    public Type type() {
        if (isCompanion()) {
            return Type.COMPANION;
        }

        if (Objects.equals(toString(), OFFICIAL_BUSINESS_ACCOUNT)) {
            return Type.OFFICIAL_BUSINESS_ACCOUNT;
        }

        return switch (server()){
            case WHATSAPP -> Type.USER;
            case BROADCAST -> Objects.equals(user(), "status") ? Type.STATUS : Type.BROADCAST;
            case GROUP -> Type.GROUP;
            case GROUP_CALL -> Type.GROUP_CALL;
            case USER -> switch (user()){
                case "server" -> Type.SERVER;
                case "0" -> Type.ANNOUNCEMENT;
                default -> Type.UNKNOWN;
            };
        };
    }

    /**
     * Returns whether this jid is associated with a companion device
     *
     * @return true if this jid is a companion
     */
    public boolean isCompanion(){
        return device() != 0;
    }

    /**
     * Checks if the input object equals this jid.
     * The equality is determined based on the server, other variables might differ.
     *
     * @param other the object to check, maybe null
     * @return true if the object matches
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof ContactJid that && this.server() == that.server();
    }

    /**
     * Returns a hash code value for the object
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(user, server, device, agent);
    }

    @Override
    public String toString() {
        if (isCompanion()) {
            var user = Objects.requireNonNullElse(user(), "");
            var agent = agent() != 0 ? "_%s".formatted(agent()) : "";
            var device = device() != 0 ? ":%s".formatted(device()) : "";
            return "%s%s%s@%s".formatted(user, agent, device, server());
        }

        if (agent != 0) {
            return "%s.%s@%s".formatted(user, agent, Server.WHATSAPP);
        }

        return user == null ? Server.WHATSAPP.address()
                : "%s@%s".formatted(user, Server.WHATSAPP);
    }

    /**
     * Checks if the input user is equal to the one wrapped by this jid
     *
     * @param user the user to check, maybe null
     * @return true if the user matches
     */
    public boolean contentEquals(String user){
        return Objects.equals(user(), user);
    }

    /**
     * Checks if the input server is equal to the one wrapped by this jid
     *
     * @param server the server to check, maybe null
     * @return true if the server matches
     */
    public boolean contentEquals(Server server){
        return Objects.equals(server(), server);
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

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Server {
        USER("c.us"),
        GROUP("g.us"),
        BROADCAST("broadcast"),
        GROUP_CALL("call"),
        WHATSAPP("s.whatsapp.net");

        @Getter
        private String address;

        @JsonCreator
        public static Server forAddress(String address) {
            return Arrays.stream(values())
                    .filter(entry -> Objects.equals(entry.address(), address))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No known server matches %s".formatted(address)));
        }

        @Override
        @JsonValue
        public String toString() {
            return address();
        }
    }
}