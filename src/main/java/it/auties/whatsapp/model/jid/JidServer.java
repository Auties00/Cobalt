package it.auties.whatsapp.model.jid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various servers that a jid might be linked
 * to
 */
public enum JidServer {
    /**
     * User
     */
    USER("c.us"),
    /**
     * Groups and communities
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
    LID("lid"),
    /**
     * Newsletter
     */
    NEWSLETTER("newsletter");

    private final String address;

    JidServer(String address) {
        this.address = address;
    }

    @JsonCreator
    public static JidServer of(String address) {
        return Arrays.stream(values())
                .filter(entry -> address != null && address.endsWith(entry.address()))
                .findFirst()
                .orElse(WHATSAPP);
    }

    public String address() {
        return address;
    }

    public Jid toJid() {
        return Jid.ofServer(this);
    }

    @Override
    @JsonValue
    public String toString() {
        return address();
    }
}
