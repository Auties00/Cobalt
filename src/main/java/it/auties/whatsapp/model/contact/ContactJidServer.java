package it.auties.whatsapp.model.contact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various servers that a jid might be linked
 * to
 */
public enum ContactJidServer {
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

    private final String address;

    ContactJidServer(String address) {
        this.address = address;
    }

    @JsonCreator
    public static ContactJidServer of(String address) {
        return Arrays.stream(values())
                .filter(entry -> address != null && address.endsWith(entry.address()))
                .findFirst()
                .orElse(WHATSAPP);
    }

    public String address() {
        return address;
    }

    public ContactJid toJid() {
        return ContactJid.ofServer(this);
    }

    @Override
    @JsonValue
    public String toString() {
        return address();
    }
}
